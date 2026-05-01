package com.aterminal.app.hosts

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import com.aterminal.app.security.HostCredentialSecret
import com.aterminal.app.security.HostCredentialStore
import com.aterminal.app.ssh.SshAuthCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HostListViewModelTest {
    @Test
    fun observesHostsFromStore() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val viewModel = HostListViewModel(
            hostStore = store,
            credentialStore = FakeHostCredentialStore(),
            scope = TestScope(dispatcher),
        )

        store.emit(listOf(host(id = 7, displayName = "Dev Box")))
        advanceUntilIdle()

        assertEquals(listOf("Dev Box"), viewModel.state.value.hosts.map { it.displayName })
    }

    @Test
    fun savesHostMetadataFromFormAndClosesDialog() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val credentialStore = FakeHostCredentialStore()
        val viewModel = HostListViewModel(
            hostStore = store,
            credentialStore = credentialStore,
            scope = TestScope(dispatcher),
            clock = { 1_234L },
        )

        viewModel.showAddHostDialog()
        viewModel.updateForm(
            HostFormState(
                displayName = "Dev Box",
                hostname = "dev.example.com",
                portText = "2222",
                username = "agent",
                authType = AuthType.PRIVATE_KEY,
                privateKeyPem = "private-key",
                privateKeyPassphrase = "passphrase",
            ),
        )
        viewModel.saveHost()
        advanceUntilIdle()

        assertEquals(
            listOf(
                HostEntity(
                    displayName = "Dev Box",
                    hostname = "dev.example.com",
                    port = 2222,
                    username = "agent",
                    authType = AuthType.PRIVATE_KEY,
                    createdAtEpochMillis = 1_234L,
                    updatedAtEpochMillis = 1_234L,
                ),
            ),
            store.createdHosts,
        )
        assertEquals(
            listOf(
                "1:PrivateKey(privateKeyPem=private-key, passphrase=passphrase)",
            ),
            credentialStore.savedCredentials,
        )
        assertFalse(viewModel.state.value.showAddHostDialog)
        assertEquals(HostFormState(), viewModel.state.value.form)
    }

    @Test
    fun savesPasswordCredentialForPasswordAuthHosts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val credentialStore = FakeHostCredentialStore()
        val viewModel = HostListViewModel(
            hostStore = store,
            credentialStore = credentialStore,
            scope = TestScope(dispatcher),
        )

        viewModel.updateForm(
            HostFormState(
                displayName = "Dev Box",
                hostname = "dev.example.com",
                portText = "22",
                username = "agent",
                authType = AuthType.PASSWORD,
                password = "ssh-password",
            ),
        )
        viewModel.saveHost()
        advanceUntilIdle()

        assertEquals(
            listOf("1:Password(password=ssh-password)"),
            credentialStore.savedCredentials,
        )
    }

    @Test
    fun missingCredentialShowsValidationErrorWithoutCreatingHost() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val viewModel = HostListViewModel(
            hostStore = store,
            credentialStore = FakeHostCredentialStore(),
            scope = TestScope(dispatcher),
        )

        viewModel.updateForm(
            HostFormState(
                displayName = "Dev Box",
                hostname = "dev.example.com",
                portText = "22",
                username = "agent",
                authType = AuthType.PRIVATE_KEY,
                privateKeyPem = "",
            ),
        )
        viewModel.saveHost()
        advanceUntilIdle()

        assertEquals(emptyList<HostEntity>(), store.createdHosts)
        assertEquals("Private key is required.", viewModel.state.value.errorMessage)
    }

    @Test
    fun invalidPortShowsValidationErrorWithoutCreatingHost() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val credentialStore = FakeHostCredentialStore()
        val viewModel = HostListViewModel(
            hostStore = store,
            credentialStore = credentialStore,
            scope = TestScope(dispatcher),
        )

        viewModel.updateForm(
            HostFormState(
                displayName = "Dev Box",
                hostname = "dev.example.com",
                portText = "70000",
                username = "agent",
                authType = AuthType.PRIVATE_KEY,
            ),
        )
        viewModel.saveHost()
        advanceUntilIdle()

        assertEquals(emptyList<HostEntity>(), store.createdHosts)
        assertEquals("Port must be between 1 and 65535.", viewModel.state.value.errorMessage)
    }

    @Test
    fun deletesHostById() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val credentialStore = FakeHostCredentialStore()
        val viewModel = HostListViewModel(
            hostStore = store,
            credentialStore = credentialStore,
            scope = TestScope(dispatcher),
        )

        viewModel.deleteHost(42)
        advanceUntilIdle()

        assertEquals(listOf(42L), store.deletedHostIds)
        assertEquals(listOf(42L), credentialStore.deletedHostIds)
    }

    @Test
    fun connectHostUsesStoredPrivateKeyCredential() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val credentialStore = FakeHostCredentialStore(
            credentials = mapOf(
                7L to HostCredentialSecret.PrivateKey(
                    privateKeyPem = "private-key",
                    passphrase = "passphrase",
                ),
            ),
        )
        val connector = RecordingHostConnector()
        val viewModel = HostListViewModel(
            hostStore = FakeHostStore(),
            credentialStore = credentialStore,
            hostConnector = connector,
            scope = TestScope(dispatcher),
        )

        viewModel.connectHost(host(id = 7, displayName = "Dev Box"))
        advanceUntilIdle()

        assertEquals(
            listOf(
                "Dev Box:PrivateKey(privateKeyPem=private-key, passphrase=passphrase)",
            ),
            connector.events,
        )
        assertEquals(7L, viewModel.state.value.connectedHostId)
        assertEquals("Connected to Dev Box.", viewModel.state.value.statusMessage)
    }

    @Test
    fun connectHostStoresDetectedRemoteCapabilities() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val capabilities = capabilities(
            tmux = capability(available = true, path = "/usr/bin/tmux", version = "tmux 3.4"),
            codex = capability(available = true, path = "/usr/local/bin/codex"),
            claude = capability(available = false),
        )
        val viewModel = HostListViewModel(
            hostStore = FakeHostStore(),
            credentialStore = FakeHostCredentialStore(
                credentials = mapOf(7L to HostCredentialSecret.Password("ssh-password")),
            ),
            hostConnector = RecordingHostConnector(capabilities = capabilities),
            scope = TestScope(dispatcher),
        )

        viewModel.connectHost(
            host(
                id = 7,
                displayName = "Dev Box",
                authType = AuthType.PASSWORD,
            ),
        )
        advanceUntilIdle()

        assertEquals(capabilities, viewModel.state.value.capabilitiesByHostId[7L])
    }

    @Test
    fun connectHostUsesStoredPasswordCredential() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connector = RecordingHostConnector()
        val viewModel = HostListViewModel(
            hostStore = FakeHostStore(),
            credentialStore = FakeHostCredentialStore(
                credentials = mapOf(8L to HostCredentialSecret.Password("ssh-password")),
            ),
            hostConnector = connector,
            scope = TestScope(dispatcher),
        )

        viewModel.connectHost(
            host(
                id = 8,
                displayName = "Password Box",
                authType = AuthType.PASSWORD,
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf("Password Box:Password(password=ssh-password)"),
            connector.events,
        )
    }

    @Test
    fun missingCredentialShowsErrorWithoutConnecting() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val connector = RecordingHostConnector()
        val viewModel = HostListViewModel(
            hostStore = FakeHostStore(),
            credentialStore = FakeHostCredentialStore(),
            hostConnector = connector,
            scope = TestScope(dispatcher),
        )

        viewModel.connectHost(host(id = 9, displayName = "Missing Secret"))
        advanceUntilIdle()

        assertEquals(emptyList<String>(), connector.events)
        assertEquals(
            "No saved credential for Missing Secret. Re-add the host credential before connecting.",
            viewModel.state.value.errorMessage,
        )
    }

    @Test
    fun connectFailureShowsUserFacingError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = HostListViewModel(
            hostStore = FakeHostStore(),
            credentialStore = FakeHostCredentialStore(
                credentials = mapOf(10L to HostCredentialSecret.Password("ssh-password")),
            ),
            hostConnector = RecordingHostConnector(
                error = IllegalStateException("network down"),
            ),
            scope = TestScope(dispatcher),
        )

        viewModel.connectHost(
            host(
                id = 10,
                displayName = "Flaky Box",
                authType = AuthType.PASSWORD,
            ),
        )
        advanceUntilIdle()

        assertEquals("network down", viewModel.state.value.errorMessage)
        assertEquals(null, viewModel.state.value.connectedHostId)
        assertFalse(viewModel.state.value.capabilitiesByHostId.containsKey(10L))
    }

    private class FakeHostStore : HostStore {
        private val hosts = MutableStateFlow<List<HostEntity>>(emptyList())
        val createdHosts = mutableListOf<HostEntity>()
        val deletedHostIds = mutableListOf<Long>()

        override fun observeHosts(): Flow<List<HostEntity>> = hosts

        override suspend fun create(host: HostEntity): Long {
            createdHosts += host
            return createdHosts.size.toLong()
        }

        override suspend fun delete(id: Long) {
            deletedHostIds += id
        }

        fun emit(value: List<HostEntity>) {
            hosts.value = value
        }
    }

    private class FakeHostCredentialStore(
        private val credentials: Map<Long, HostCredentialSecret> = emptyMap(),
    ) : HostCredentialStore {
        val savedCredentials = mutableListOf<String>()
        val deletedHostIds = mutableListOf<Long>()

        override suspend fun saveCredential(
            hostId: Long,
            credential: HostCredentialSecret,
        ) {
            savedCredentials += "$hostId:$credential"
        }

        override suspend fun getCredential(
            hostId: Long,
            authType: AuthType,
        ): HostCredentialSecret? = credentials[hostId]

        override suspend fun deleteCredential(hostId: Long) {
            deletedHostIds += hostId
        }
    }

    private class RecordingHostConnector(
        private val error: Throwable? = null,
        private val capabilities: RemoteCapabilities = capabilities(),
    ) : HostConnector {
        val events = mutableListOf<String>()

        override suspend fun connect(
            host: HostEntity,
            credential: SshAuthCredential,
        ): HostConnectionResult {
            error?.let { throw it }
            events += "${host.displayName}:$credential"
            return HostConnectionResult(capabilities)
        }
    }

    private companion object {
        fun capabilities(
            tmux: RemoteToolCapability = capability(),
            codex: RemoteToolCapability = capability(),
            claude: RemoteToolCapability = capability(),
        ) = RemoteCapabilities(
            tmux = tmux,
            codex = codex,
            claude = claude,
        )

        fun capability(
            available: Boolean = true,
            path: String? = null,
            version: String? = null,
        ) = RemoteToolCapability(
            available = available,
            path = path,
            version = version,
        )
    }

    private fun host(
        id: Long,
        displayName: String,
        authType: AuthType = AuthType.PRIVATE_KEY,
    ) = HostEntity(
        id = id,
        displayName = displayName,
        hostname = "dev.example.com",
        port = 22,
        username = "agent",
        authType = authType,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
