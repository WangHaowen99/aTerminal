package com.aterminal.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentSessionDao {
    @Query(
        """
        SELECT * FROM agent_sessions
        WHERE workspace_id = :workspaceId
        ORDER BY last_attached_at_epoch_millis DESC
        """,
    )
    fun observeForWorkspace(workspaceId: Long): Flow<List<AgentSessionEntity>>

    @Query("SELECT * FROM agent_sessions WHERE id = :id")
    suspend fun getById(id: Long): AgentSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: AgentSessionEntity): Long

    @Update
    suspend fun update(session: AgentSessionEntity): Int

    @Query("DELETE FROM agent_sessions WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
