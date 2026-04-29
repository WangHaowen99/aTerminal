package com.aterminal.app.tmux

import com.aterminal.app.ssh.SshCommandResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TmuxRepositoryTest {
    @Test
    fun listSessionsParsesTmuxOutput() = runTest {
        val control = RecordingTmuxControlChannel(
            results = mapOf(
                "tmux list-sessions -F '#{session_name}\\t#{session_created}\\t#{session_attached}\\t#{session_windows}'" to
                    commandResult(
                        stdout = """
                            aterm:codex:backend-api	1710000000	1	2
                            aterm:claude:mobile	1710000100	0	1
                            malformed
                        """.trimIndent(),
                    ),
            ),
        )
        val repository = TmuxRepository(control)

        val sessions = repository.listSessions()

        assertEquals(
            listOf(
                TmuxSession(
                    name = "aterm:codex:backend-api",
                    createdAtEpochSeconds = 1_710_000_000,
                    attached = true,
                    windowCount = 2,
                ),
                TmuxSession(
                    name = "aterm:claude:mobile",
                    createdAtEpochSeconds = 1_710_000_100,
                    attached = false,
                    windowCount = 1,
                ),
            ),
            sessions,
        )
    }

    @Test
    fun repositoryExecutesSessionManagementCommands() = runTest {
        val control = RecordingTmuxControlChannel()
        val repository = TmuxRepository(control)

        repository.attachSession("aterm:codex:backend-api")
        repository.killSession("aterm:codex:backend-api")
        repository.detachClient()

        assertEquals(
            listOf(
                "tmux attach-session -t 'aterm:codex:backend-api'",
                "tmux kill-session -t 'aterm:codex:backend-api'",
                "tmux detach-client",
            ),
            control.commands,
        )
    }

    private class RecordingTmuxControlChannel(
        private val results: Map<String, SshCommandResult> = emptyMap(),
    ) : TmuxControlChannel {
        val commands = mutableListOf<String>()

        override suspend fun execute(
            command: String,
            timeoutMillis: Long,
        ): SshCommandResult {
            commands += command
            return results[command] ?: commandResult(command = command)
        }
    }

    private companion object {
        fun commandResult(
            command: String = "tmux",
            stdout: String = "",
            stderr: String = "",
            exitStatus: Int = 0,
        ) = SshCommandResult(
            command = command,
            exitStatus = exitStatus,
            stdout = stdout,
            stderr = stderr,
        )
    }
}
