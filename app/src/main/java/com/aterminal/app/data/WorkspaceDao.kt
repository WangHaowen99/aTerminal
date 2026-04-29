package com.aterminal.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query(
        """
        SELECT * FROM workspaces
        WHERE host_id = :hostId
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observeForHost(hostId: Long): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getById(id: Long): WorkspaceEntity?

    @Query("SELECT * FROM workspaces ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllSnapshot(): List<WorkspaceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workspace: WorkspaceEntity): Long

    @Update
    suspend fun update(workspace: WorkspaceEntity): Int

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
