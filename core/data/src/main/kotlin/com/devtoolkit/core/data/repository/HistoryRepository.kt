package com.devtoolkit.core.data.repository

import com.devtoolkit.core.data.db.HistoryDao
import com.devtoolkit.core.data.db.HistoryEntity
import com.devtoolkit.core.domain.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
) {
    fun getAll(): Flow<List<HistoryEntry>> = dao.getAll().map { list -> list.map { it.toDomain() } }

    fun getByTool(toolId: String): Flow<List<HistoryEntry>> =
        dao.getByTool(toolId).map { list -> list.map { it.toDomain() } }

    fun getStarred(): Flow<List<HistoryEntry>> =
        dao.getStarred().map { list -> list.map { it.toDomain() } }

    fun search(query: String): Flow<List<HistoryEntry>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    fun filterEntries(query: String, toolId: String?, starredOnly: Boolean): Flow<List<HistoryEntry>> =
        dao.filterEntries(query = query, toolId = toolId, starredOnly = if (starredOnly) 1 else 0)
            .map { list -> list.map { it.toDomain() } }

    suspend fun addEntry(toolId: String, input: String, output: String) {
        dao.insert(
            HistoryEntity(
                toolId = toolId,
                input = input,
                output = output,
                timestamp = System.currentTimeMillis(),
            )
        )
        dao.pruneOldEntries(toolId)
    }

    suspend fun toggleStar(entry: HistoryEntry) {
        dao.update(entry.toEntity().copy(isStarred = !entry.isStarred))
    }

    suspend fun deleteEntry(entry: HistoryEntry) {
        dao.delete(entry.toEntity())
    }

    private fun HistoryEntity.toDomain() = HistoryEntry(id, toolId, input, output, timestamp, isStarred)
    private fun HistoryEntry.toEntity() = HistoryEntity(id, toolId, input, output, timestamp, isStarred)
}
