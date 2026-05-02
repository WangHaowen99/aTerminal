package com.aterminal.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.aterminal.app.data.AppDatabase
import com.aterminal.app.hosts.ActiveHostSessionStore
import com.aterminal.app.hosts.HostRepository
import com.aterminal.app.hosts.SshHostConnector
import com.aterminal.app.metadata.MetadataTransferRepository
import com.aterminal.app.security.SecretStore
import com.aterminal.app.settings.AgentSettingsRepository

private val Context.agentSettingsDataStore by preferencesDataStore(
    name = "agent_settings",
)

class AterminalApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aterminal.db",
        ).build()
    }

    val agentSettingsRepository: AgentSettingsRepository by lazy {
        AgentSettingsRepository(agentSettingsDataStore)
    }

    val hostRepository: HostRepository by lazy {
        HostRepository(database.hostDao())
    }

    val secretStore: SecretStore by lazy {
        SecretStore(applicationContext)
    }

    val hostConnector: SshHostConnector by lazy {
        SshHostConnector()
    }

    val activeHostSessionStore: ActiveHostSessionStore by lazy {
        ActiveHostSessionStore()
    }

    val metadataTransferRepository: MetadataTransferRepository by lazy {
        MetadataTransferRepository(
            hostDao = database.hostDao(),
            workspaceDao = database.workspaceDao(),
        )
    }
}
