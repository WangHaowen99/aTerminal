package com.aterminal.app.errors

import com.aterminal.app.agents.AgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFacingErrorMessageTest {
    @Test
    fun mapsSshAuthenticationFailureToActionableMessage() {
        val message = UserFacingErrorMessage.from(
            SshAuthenticationFailedException(),
        )

        assertEquals(
            "SSH authentication failed. Check the username, password, private key, or passphrase.",
            message,
        )
    }

    @Test
    fun mapsHostKeyMismatchToSafetyMessageWithFingerprints() {
        val message = UserFacingErrorMessage.from(
            HostKeyMismatchException(
                expectedFingerprint = "SHA256:old",
                actualFingerprint = "SHA256:new",
            ),
        )

        assertTrue(message.contains("Host key changed for this host."))
        assertTrue(message.contains("expected SHA256:old"))
        assertTrue(message.contains("saw SHA256:new"))
    }

    @Test
    fun mapsMissingRemoteToolsToInstallGuidance() {
        assertEquals(
            "tmux is not installed on the remote host. Install tmux before launching persistent agent sessions.",
            UserFacingErrorMessage.from(MissingRemoteToolException(RemoteTool.TMUX)),
        )
        assertEquals(
            "Codex is not installed on the remote host. Install the codex CLI or choose Shell/Claude.",
            UserFacingErrorMessage.from(MissingRemoteToolException(RemoteTool.CODEX)),
        )
        assertEquals(
            "Claude Code is not installed on the remote host. Install claude or choose Shell/Codex.",
            UserFacingErrorMessage.from(MissingRemoteToolException(RemoteTool.CLAUDE)),
        )
    }

    @Test
    fun mapsTmuxCommandFailureWithoutExposingOnlyRawExitCode() {
        val message = UserFacingErrorMessage.from(
            TmuxCommandFailedException(
                command = "tmux attach-session -t 'aterm:codex:missing'",
                exitStatus = 1,
                stderr = "can't find session: missing",
            ),
        )

        assertEquals(
            "tmux command failed: can't find session: missing",
            message,
        )
    }

    @Test
    fun mapsWorkspaceAgentAvailabilityMessagesThroughSharedCatalog() {
        assertEquals(
            "Codex is not installed on the remote host. Install the codex CLI or choose Shell/Claude.",
            UserFacingErrorMessage.missingAgent(AgentType.CODEX),
        )
        assertEquals(
            "Claude Code is not installed on the remote host. Install claude or choose Shell/Codex.",
            UserFacingErrorMessage.missingAgent(AgentType.CLAUDE),
        )
    }
}
