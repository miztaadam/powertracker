package com.powertracker.data.db

import androidx.room.TypeConverter
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

/** Column encodings: enums as name TEXT, LocalDate as epochDay, DayOfWeek as 1-7, lists as JSON. */
class Converters {
    @TypeConverter fun dateToDb(d: LocalDate): Long = d.toEpochDay()
    @TypeConverter fun dateFromDb(v: Long): LocalDate = LocalDate.ofEpochDay(v)

    @TypeConverter fun weekdayToDb(d: DayOfWeek): Int = d.value
    @TypeConverter fun weekdayFromDb(v: Int): DayOfWeek = DayOfWeek.of(v)

    @TypeConverter fun categoryToDb(c: Category): String = c.name
    @TypeConverter fun categoryFromDb(v: String): Category = Category.valueOf(v)

    @TypeConverter fun liftToDb(l: MainLift?): String? = l?.name
    @TypeConverter fun liftFromDb(v: String?): MainLift? = v?.let(MainLift::valueOf)

    @TypeConverter fun missToDb(m: MissPoint?): String? = m?.name
    @TypeConverter fun missFromDb(v: String?): MissPoint? = v?.let(MissPoint::valueOf)

    @TypeConverter fun adviceTypeToDb(t: AdviceType): String = t.name
    @TypeConverter fun adviceTypeFromDb(v: String): AdviceType = AdviceType.valueOf(v)

    @TypeConverter fun adviceStatusToDb(s: AdviceStatus): String = s.name
    @TypeConverter fun adviceStatusFromDb(v: String): AdviceStatus = AdviceStatus.valueOf(v)

    @TypeConverter fun stringsToDb(l: List<String>): String = Json.encodeToString(strings, l)
    @TypeConverter fun stringsFromDb(v: String): List<String> = Json.decodeFromString(strings, v)

    private companion object {
        val strings = ListSerializer(String.serializer())
    }
}
