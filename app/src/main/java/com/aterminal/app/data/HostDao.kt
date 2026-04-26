package com.aterminal.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {
    @Query("SELECT * FROM hosts ORDER BY display_name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<HostEntity>>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getById(id: Long): HostEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(host: HostEntity): Long

    @Update
    suspend fun update(host: HostEntity): Int

    @Query("DELETE FROM hosts WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
