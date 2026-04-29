package com.aterminal.app.security

import com.aterminal.app.data.HostEntity

class HostFingerprintStore {
    fun check(
        pinnedFingerprint: String?,
        presentedFingerprint: String,
    ): HostFingerprintCheck {
        val presented = normalizeFingerprint(presentedFingerprint)
        val pinned = pinnedFingerprint?.trim()?.takeIf { it.isNotEmpty() }

        return when {
            pinned == null -> HostFingerprintCheck(
                state = HostFingerprintState.UNKNOWN,
                pinnedFingerprint = null,
                presentedFingerprint = presented,
                requiresConfirmation = true,
                blocksConnection = true,
            )

            pinned == presented -> HostFingerprintCheck(
                state = HostFingerprintState.MATCH,
                pinnedFingerprint = pinned,
                presentedFingerprint = presented,
                requiresConfirmation = false,
                blocksConnection = false,
            )

            else -> HostFingerprintCheck(
                state = HostFingerprintState.CHANGED,
                pinnedFingerprint = pinned,
                presentedFingerprint = presented,
                requiresConfirmation = true,
                blocksConnection = true,
            )
        }
    }

    fun confirm(
        host: HostEntity,
        presentedFingerprint: String,
        updatedAtEpochMillis: Long,
    ): HostEntity {
        return host.copy(
            pinnedFingerprint = normalizeFingerprint(presentedFingerprint),
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    private fun normalizeFingerprint(fingerprint: String): String {
        val normalized = fingerprint.trim()
        require(normalized.isNotEmpty()) { "Host fingerprint must not be blank." }
        return normalized
    }
}

data class HostFingerprintCheck(
    val state: HostFingerprintState,
    val pinnedFingerprint: String?,
    val presentedFingerprint: String,
    val requiresConfirmation: Boolean,
    val blocksConnection: Boolean,
)

enum class HostFingerprintState {
    MATCH,
    UNKNOWN,
    CHANGED,
}
