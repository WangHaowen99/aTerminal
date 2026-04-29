package com.aterminal.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aterminal.app.agents.AgentType

@Entity(
    tableName = "workspaces",
    foreignKeys = [
        ForeignKey(
            entity = HostEntity::class,
            parentColumns = ["id"],
            childColumns = ["host_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("host_id"),
        Index(value = ["host_id", "name"], unique = true),
    ],
)
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "host_id")
    val hostId: Long,
    val name: String,
    @ColumnInfo(name = "remote_cwd")
    val remoteCwd: String,
    @ColumnInfo(name = "preferred_agent")
    val preferredAgent: AgentType,
    @ColumnInfo(name = "agent_flags")
    val agentFlags: String? = null,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)
