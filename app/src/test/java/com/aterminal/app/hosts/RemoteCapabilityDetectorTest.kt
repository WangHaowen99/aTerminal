package com.aterminal.app.hosts

import com.aterminal.app.ssh.SshCommandResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCapabilityDetectorTest {
    @Test
    fun detectsTmuxCodexClaudeAndTmuxVersion() = runTest {
        val channel = RecordingCapabilityChannel(
            results = mapOf(
                "command -v tmux" to success("/usr/bin/tmux\n"),
                "tmux -V" to success("tmux 3.4\n"),
                "command -v codex" to success("/usr/local/bin/codex\n"),
                "command -v claude" to success("/opt/bin/claude\n"),
            ),
        )
        val detector = RemoteCapabilityDetector(channel)

        val capabilities = detector.detect()

        assertEquals(
            listOf(
                "command -v tmux",
                "tmux -V",
                "command -v codex",
                "command -v claude",
            ),
            channel.commands,
        )
        assertTrue(capabilities.tmux.available)
        assertEquals("/usr/bin/tmux", capabilities.tmux.path)
        assertEquals("tmux 3.4", capabilities.tmux.version)
        assertTrue(capabilities.codex.available)
        assertEquals("/usr/local/bin/codex", capabilities.codex.path)
        assertTrue(capabilities.claude.available)
        assertEquals("/opt/bin/claude", capabilities.claude.path)
        assertNull(capabilities.tmuxInstallGuidance)
    }

    @Test
    fun reportsMissingToolsAndSkipsTmuxVersionWhenTmuxUnavailable() = runTest {
        val channel = RecordingCapabilityChannel(
            results = mapOf(
                "command -v tmux" to failure(),
                "command -v codex" to failure(),
                "command -v claude" to failure(),
            ),
        )
        val detector = RemoteCapabilityDetector(channel)

        val capabilities = detector.detect()

        assertEquals(
            listOf(
                "command -v tmux",
                "command -v codex",
                "command -v claude",
            ),
            channel.commands,
        )
        assertFalse(capabilities.tmux.available)
        assertFalse(capabilities.codex.available)
        assertFalse(capabilities.claude.available)
        assertEquals(
            "Install tmux on the remote host before launching persistent agent sessions.",
            capabilities.tmuxInstallGuidance,
        )
    }

    @Test
    fun treatsBlankCommandOutputAsUnavailable() = runTest {
        val channel = RecordingCapabilityChannel(
            results = mapOf(
                "command -v tmux" to success("\n"),
                "command -v codex" to success("  \n"),
                "command -v claude" to failure(),
            ),
        )

        val capabilities = RemoteCapabilityDetector(channel).detect()

        assertFalse(capabilities.tmux.available)
        assertFalse(capabilities.codex.available)
        assertFalse(capabilities.claude.available)
    }

    private class RecordingCapabilityChannel(
        private val results: Map<String, SshCommandResult>,
    ) : RemoteCapabilityChannel {
        val commands = mutableListOf<String>()

        override suspend fun execute(command: String, timeoutMillis: Long): SshCommandResult {
            commands += command
            return results.getValue(command)
        }
    }

    private fun success(stdout: String) = SshCommandResult(
        command = "",
        exitStatus = 0,
        stdout = stdout,
        stderr = "",
    )

    private fun failure() = SshCommandResult(
        command = "",
        exitStatus = 1,
        stdout = "",
        stderr = "missing",
    )
}
