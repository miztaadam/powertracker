package com.powertracker.data

import com.powertracker.data.db.AdviceEntity
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Open so tests can fake a failing write. */
open class AdviceRepository(private val db: AppDatabase) {
    fun open(): Flow<List<Advice>> = db.advice().observeOpen().map { list -> list.map { it.toDomain() } }

    suspend fun all(): List<Advice> = db.advice().all().map { it.toDomain() }

    suspend fun insert(advice: List<Advice>) = db.advice().insert(advice.map { it.toEntity() })

    open suspend fun setStatus(id: Long, status: AdviceStatus) = db.advice().setStatus(id, status)
}

private fun Advice.toEntity() = AdviceEntity(
    id, type, lift, createdAt, reason, severity, Json.encodeToString<AdvicePayload>(payload), status, mode,
)

private fun AdviceEntity.toDomain() = Advice(
    type, lift, createdAt, reason, severity, Json.decodeFromString<AdvicePayload>(payload), status, id, mode,
)
