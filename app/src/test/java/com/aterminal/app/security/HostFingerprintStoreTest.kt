package com.aterminal.app.security

import com.aterminal.app.data.AuthType
import com.aterminal.app.data.HostEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostFingerprintStoreTest {
    private val store = HostFingerprintStore()

    @Test
    fun matchingFingerprintIsTrustedWithoutConfirmation() {
        val check = store.check(
            pinnedFingerprint = "SHA256:abc123",
            presentedFingerprint = "SHA256:abc123",
        )

        assertEquals(HostFingerprintState.MATCH, check.state)
        assertFalse(check.requiresConfirmation)
        assertFalse(check.blocksConnection)
    }

    @Test
    fun missingFingerprintRequiresConfirmationBeforeTrustingHost() {
        val check = store.check(
            pinnedFingerprint = null,
            presentedFingerprint = "SHA256:first-seen",
        )

        assertEquals(HostFingerprintState.UNKNOWN, check.state)
        assertTrue(check.requiresConfirmation)
        assertTrue(check.blocksConnection)

        val confirmedHost = store.confirm(
            host = hostFixture(pinnedFingerprint = null),
            presentedFingerprint = "SHA256:first-seen",
            updatedAtEpochMillis = 300,
        )

        assertEquals("SHA256:first-seen", confirmedHost.pinnedFingerprint)
        assertEquals(300, confirmedHost.updatedAtEpochMillis)
    }

    @Test
    fun changedFingerprintBlocksConnectionUntilUserConfirms() {
        val check = store.check(
            pinnedFingerprint = "SHA256:old",
            presentedFingerprint = "SHA256:new",
        )

        assertEquals(HostFingerprintState.CHANGED, check.state)
        assertTrue(check.requiresConfirmation)
        assertTrue(check.blocksConnection)
        assertEquals("SHA256:old", check.pinnedFingerprint)
        assertEquals("SHA256:new", check.presentedFingerprint)
    }

    private fun hostFixture(pinnedFingerprint: String?) = HostEntity(
        displayName = "Dev Box",
        hostname = "dev.example.com",
        port = 22,
        username = "agent",
        authType = AuthType.PRIVATE_KEY,
        pinnedFingerprint = pinnedFingerprint,
        createdAtEpochMillis = 100,
        updatedAtEpochMillis = 100,
    )
}
