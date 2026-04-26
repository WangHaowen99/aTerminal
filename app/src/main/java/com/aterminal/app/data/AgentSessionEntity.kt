package com.aterminal.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aterminal.app.agents.AgentType

@Entity(
    tableName = "agent_sessions",
    foreignKeys = [
        ForeignKey(
            entity = HostEntity::class,
            parentColumns = ["id"],
            childColumns = ["host_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspace_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("host_id"),
        Index("workspace_id"),
        Index(value = ["workspace_id", "agent_type", "tmux_session_name"], unique = true),
    ],
)
data class AgentSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "host_id")
    val hostId: Long,
    @ColumnInfo(name = "workspace_id")
    val workspaceId: Long,
    @ColumnInfo(name = "agent_type")
    val agentType: AgentType,
    @ColumnInfo(name = "tmux_session_name")
    val tmuxSessionName: String,
    @ColumnInfo(name = "agent_session_ref")
    val agentSessionRef: String? = null,
    @ColumnInfo(name = "last_attached_at_epoch_millis")
    val lastAttachedAtEpochMillis: Long,
)
