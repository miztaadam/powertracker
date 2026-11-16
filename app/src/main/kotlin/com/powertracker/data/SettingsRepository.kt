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
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

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
    /** Strict press on Progress and in the total (strict press spec §4.2). Coaching ignores it. */
    val showStrictPress: Boolean = false,
    /** The side the app is on (bodybuilding mode spec §6). Off is the powerlifting side. */
    val trainingMode: TrainingMode = TrainingMode.POWERLIFTING,
    /**
     * The bodybuilding side's training days; [trainingDays] stays the powerlifting side's. Copied from [trainingDays]
     * the first time the bodybuilding side is entered (see [withTrainingMode]).
     */
    val bodybuildingTrainingDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    /** The day each side was last left; null until it is first left. */
    val powerliftingLeftOn: LocalDate? = null,
    val bodybuildingLeftOn: LocalDate? = null,
    /**
     * "Vibrate on taps" (stage 2): a short buzz on the main actions, apart from [restAlert]. A setting of this phone
     * only: it is not in the backup (owner, option A), so a restore leaves it on, its default.
     */
    val vibrateOnTaps: Boolean = true,
) {
    fun coach(): CoachSettings = CoachSettings(defaultRpe = defaultRpe, changePlates = changePlates)

    /** The training days of the side the app is on. */
    val activeTrainingDays: Set<DayOfWeek>
        get() = if (trainingMode == TrainingMode.BODYBUILDING) bodybuildingTrainingDays else trainingDays

    /** [days] as the training days of the side the app is on; the other side's days are kept. */
    fun withActiveTrainingDays(days: Set<DayOfWeek>): AppSettings =
        if (trainingMode == TrainingMode.BODYBUILDING) copy(bodybuildingTrainingDays = days) else copy(trainingDays = days)

    fun leftOn(mode: TrainingMode): LocalDate? = when (mode) {
        TrainingMode.POWERLIFTING -> powerliftingLeftOn
        TrainingMode.BODYBUILDING -> bodybuildingLeftOn
    }

    /**
     * On [mode]'s side. The side being left is stamped with [today]. The first time the bodybuilding side is entered (it
     * has never been left, so it has never been on) its days start as a copy of the powerlifting side's. Switching to
     * the side already on changes nothing.
     */
    fun withTrainingMode(mode: TrainingMode, today: LocalDate): AppSettings {
        if (mode == trainingMode) return this
        val left = when (trainingMode) {
            TrainingMode.POWERLIFTING -> copy(powerliftingLeftOn = today)
            TrainingMode.BODYBUILDING -> copy(bodybuildingLeftOn = today)
        }
        val days = if (mode == TrainingMode.BODYBUILDING && bodybuildingLeftOn == null) trainingDays else bodybuildingTrainingDays
        return left.copy(trainingMode = mode, bodybuildingTrainingDays = days)
    }

    /** [mode]'s side as if never left (its welcome back has been answered). */
    fun withoutLeftOn(mode: TrainingMode): AppSettings = when (mode) {
        TrainingMode.POWERLIFTING -> copy(powerliftingLeftOn = null)
        TrainingMode.BODYBUILDING -> copy(bodybuildingLeftOn = null)
    }
}

// One DataStore per process: the delegate caches the instance on the application context.
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { it.toAppSettings() }

    val showStrictPress: Flow<Boolean> = settings.map { it.showStrictPress }.distinctUntilChanged()

    suspend fun setShowStrictPress(on: Boolean) = update { it.copy(showStrictPress = on) }

    /** Moves the app to [mode]'s side; see [AppSettings.withTrainingMode]. */
    suspend fun setTrainingMode(mode: TrainingMode, today: LocalDate) = update { it.withTrainingMode(mode, today) }

    /**
     * Forgets the day [mode]'s side was last left. Call it only while that side is on: leaving it stamps the day again.
     * Cleared for the bodybuilding side while the powerlifting side is on, the next switch would copy the days again.
     */
    suspend fun clearLeftOn(mode: TrainingMode) = update { it.withoutLeftOn(mode) }

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
        val SHOW_STRICT_PRESS = booleanPreferencesKey("showStrictPress")
        val TRAINING_MODE = stringPreferencesKey("trainingMode")
        val BODYBUILDING_TRAINING_DAYS = stringPreferencesKey("bodybuildingTrainingDays")
        /** ISO dates (YYYY-MM-DD); absent until the side is first left. */
        val POWERLIFTING_LEFT_ON = stringPreferencesKey("powerliftingLeftOn")
        val BODYBUILDING_LEFT_ON = stringPreferencesKey("bodybuildingLeftOn")
        val VIBRATE_ON_TAPS = booleanPreferencesKey("vibrateOnTaps")

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
                showStrictPress = this[SHOW_STRICT_PRESS] ?: d.showStrictPress,
                trainingMode = this[TRAINING_MODE]?.let { name -> TrainingMode.entries.firstOrNull { it.name == name } } ?: d.trainingMode,
                bodybuildingTrainingDays = this[BODYBUILDING_TRAINING_DAYS]?.let(::parseDays) ?: d.bodybuildingTrainingDays,
                powerliftingLeftOn = this[POWERLIFTING_LEFT_ON]?.let(::parseDate),
                bodybuildingLeftOn = this[BODYBUILDING_LEFT_ON]?.let(::parseDate),
                vibrateOnTaps = this[VIBRATE_ON_TAPS] ?: d.vibrateOnTaps,
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
            this[SHOW_STRICT_PRESS] = s.showStrictPress
            this[TRAINING_MODE] = s.trainingMode.name
            this[BODYBUILDING_TRAINING_DAYS] = s.bodybuildingTrainingDays.sorted().joinToString(",") { it.name }
            writeDate(POWERLIFTING_LEFT_ON, s.powerliftingLeftOn)
            writeDate(BODYBUILDING_LEFT_ON, s.bodybuildingLeftOn)
            this[VIBRATE_ON_TAPS] = s.vibrateOnTaps
        }

        fun androidx.datastore.preferences.core.MutablePreferences.writeDate(key: Preferences.Key<String>, date: LocalDate?) {
            if (date != null) this[key] = date.toString() else remove(key)
        }

        fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

        fun parseDays(text: String): Set<DayOfWeek> =
            text.split(",").filter { it.isNotBlank() }.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
    }
}
