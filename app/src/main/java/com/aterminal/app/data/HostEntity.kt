package com.aterminal.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hosts",
    indices = [
        Index(value = ["hostname", "port", "username"]),
    ],
)
data class HostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    @ColumnInfo(name = "auth_type")
    val authType: AuthType,
    @ColumnInfo(name = "pinned_fingerprint")
    val pinnedFingerprint: String? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)
