package com.aterminal.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        HostEntity::class,
        WorkspaceEntity::class,
        AgentSessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao

    abstract fun workspaceDao(): WorkspaceDao

    abstract fun agentSessionDao(): AgentSessionDao
}
