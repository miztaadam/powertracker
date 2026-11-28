package com.powertracker.data.db

import androidx.room.TypeConverter
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.SplitSides
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Column encodings: enums as name TEXT, LocalDate as epochDay, DayOfWeek as 1-7, lists as JSON. */
class Converters {
    @TypeConverter fun dateToDb(d: LocalDate): Long = d.toEpochDays()
    @TypeConverter fun dateFromDb(v: Long): LocalDate = LocalDate.fromEpochDays(v)

    @TypeConverter fun weekdayToDb(d: DayOfWeek): Int = d.isoDayNumber
    @TypeConverter fun weekdayFromDb(v: Int): DayOfWeek = DayOfWeek(v)

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

    @TypeConverter fun modeToDb(m: TrainingMode): String = m.name
    @TypeConverter fun modeFromDb(v: String): TrainingMode = TrainingMode.valueOf(v)

    @TypeConverter fun loadUnitToDb(u: LoadUnit): String = u.name
    @TypeConverter fun loadUnitFromDb(v: String): LoadUnit = LoadUnit.valueOf(v)

    @TypeConverter fun plateCountToDb(p: PlateCount): String = p.name
    @TypeConverter fun plateCountFromDb(v: String): PlateCount = PlateCount.valueOf(v)

    @TypeConverter fun skipReasonToDb(r: SkipReason?): String? = r?.name
    @TypeConverter fun skipReasonFromDb(v: String?): SkipReason? = v?.let(SkipReason::valueOf)

    @TypeConverter fun splitSidesToDb(s: SplitSides?): String? = s?.let { Json.encodeToString(SplitSides.serializer(), it) }
    @TypeConverter fun splitSidesFromDb(v: String?): SplitSides? = v?.let { Json.decodeFromString(SplitSides.serializer(), it) }

    @TypeConverter fun stringsToDb(l: List<String>): String = Json.encodeToString(strings, l)
    @TypeConverter fun stringsFromDb(v: String): List<String> = Json.decodeFromString(strings, v)

    private companion object {
        val strings = ListSerializer(String.serializer())
    }
}
