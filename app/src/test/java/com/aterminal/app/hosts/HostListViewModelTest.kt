package com.aterminal.app.hosts

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
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
        val viewModel = HostListViewModel(
            hostStore = store,
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
        assertFalse(viewModel.state.value.showAddHostDialog)
        assertEquals(HostFormState(), viewModel.state.value.form)
    }

    @Test
    fun invalidPortShowsValidationErrorWithoutCreatingHost() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = FakeHostStore()
        val viewModel = HostListViewModel(
            hostStore = store,
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
        val viewModel = HostListViewModel(
            hostStore = store,
            scope = TestScope(dispatcher),
        )

        viewModel.deleteHost(42)
        advanceUntilIdle()

        assertEquals(listOf(42L), store.deletedHostIds)
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

    private fun host(id: Long, displayName: String) = HostEntity(
        id = id,
        displayName = displayName,
        hostname = "dev.example.com",
        port = 22,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
