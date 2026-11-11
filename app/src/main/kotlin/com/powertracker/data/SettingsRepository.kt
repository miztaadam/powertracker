package com.powertracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.powertracker.domain.coach.CoachSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek

/** How the end of a rest is announced. */
enum class RestAlert { VIBRATE, SOUND_AND_VIBRATE, OFF }

data class AppSettings(
    val changePlates: Boolean = false,
    val defaultRpe: Double = 8.0,
    val trainingDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val appLock: Boolean = false,
    val restMainSec: Int = 180,
    val restAccessorySec: Int = 90,
    val restAlert: RestAlert = RestAlert.VIBRATE,
    /** Written by the web app's rest alert setting; kept as an opaque string so a backup round-trips it untouched. */
    val webRestAlert: String? = null,
) {
    fun coach(): CoachSettings = CoachSettings(defaultRpe = defaultRpe, changePlates = changePlates)
}

// One DataStore per process: the delegate caches the instance on the application context.
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { it.toAppSettings() }

    suspend fun update(f: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs.write(f(prefs.toAppSettings())) }
    }

    private companion object {
        val CHANGE_PLATES = booleanPreferencesKey("changePlates")
        val DEFAULT_RPE = doublePreferencesKey("defaultRpe")
        val TRAINING_DAYS = stringPreferencesKey("trainingDays")
        val APP_LOCK = booleanPreferencesKey("appLock")
        val REST_MAIN = intPreferencesKey("restMainSec")
        val REST_ACCESSORY = intPreferencesKey("restAccessorySec")
        val REST_ALERT = stringPreferencesKey("restAlert")
        val WEB_REST_ALERT = stringPreferencesKey("webRestAlert")

        fun Preferences.toAppSettings(): AppSettings {
            val d = AppSettings()
            return AppSettings(
                changePlates = this[CHANGE_PLATES] ?: d.changePlates,
                defaultRpe = this[DEFAULT_RPE] ?: d.defaultRpe,
                trainingDays = this[TRAINING_DAYS]?.let(::parseDays) ?: d.trainingDays,
                appLock = this[APP_LOCK] ?: d.appLock,
                restMainSec = this[REST_MAIN] ?: d.restMainSec,
                restAccessorySec = this[REST_ACCESSORY] ?: d.restAccessorySec,
                restAlert = this[REST_ALERT]?.let { name -> RestAlert.entries.firstOrNull { it.name == name } } ?: d.restAlert,
                webRestAlert = this[WEB_REST_ALERT],
            )
        }

        fun androidx.datastore.preferences.core.MutablePreferences.write(s: AppSettings) {
            this[CHANGE_PLATES] = s.changePlates
            this[DEFAULT_RPE] = s.defaultRpe
            this[TRAINING_DAYS] = s.trainingDays.sorted().joinToString(",") { it.name }
            this[APP_LOCK] = s.appLock
            this[REST_MAIN] = s.restMainSec
            this[REST_ACCESSORY] = s.restAccessorySec
            this[REST_ALERT] = s.restAlert.name
            val web = s.webRestAlert
            if (web != null) this[WEB_REST_ALERT] = web else remove(WEB_REST_ALERT)
        }

        fun parseDays(text: String): Set<DayOfWeek> =
            text.split(",").filter { it.isNotBlank() }.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
    }
}
