package com.devtoolkit.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: String,
    val input: String,
    val output: String,
    val timestamp: Long,
    val isStarred: Boolean = false,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY isStarred DESC, timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE toolId = :toolId ORDER BY isStarred DESC, timestamp DESC LIMIT 20")
    fun getByTool(toolId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarred(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE input LIKE '%' || :query || '%' OR output LIKE '%' || :query || '%' ORDER BY isStarred DESC, timestamp DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query(
        """
        SELECT * FROM history
        WHERE (:starredOnly = 0 OR isStarred = 1)
          AND (:toolId IS NULL OR toolId = :toolId)
          AND (:query = '' OR input LIKE '%' || :query || '%' OR output LIKE '%' || :query || '%')
        ORDER BY isStarred DESC, timestamp DESC
        """
    )
    fun filterEntries(query: String, toolId: String?, starredOnly: Int): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Update
    suspend fun update(entry: HistoryEntity)

    @Delete
    suspend fun delete(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE toolId = :toolId AND id NOT IN (SELECT id FROM history WHERE toolId = :toolId ORDER BY timestamp DESC LIMIT 20)")
    suspend fun pruneOldEntries(toolId: String)
}
