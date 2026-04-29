package com.aterminal.app.ssh

import com.aterminal.app.errors.SshAuthenticationFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.password.PasswordUtils
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class SshjTransport(
    private val client: SSHClient,
) : SshTransport {
    override suspend fun connect(host: SshHostConfig) {
        withContext(Dispatchers.IO) {
            client.connect(host.hostname, host.port)
        }
    }

    override suspend fun authenticate(username: String, credential: SshAuthCredential) {
        withContext(Dispatchers.IO) {
            try {
                when (credential) {
                    is SshAuthCredential.Password -> {
                        client.authPassword(username, credential.password)
                    }

                    is SshAuthCredential.PrivateKey -> {
                        client.authPublickey(username, keyProviderFromPem(credential))
                    }
                }
            } catch (error: UserAuthException) {
                throw SshAuthenticationFailedException(error)
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            client.disconnect()
        }
    }

    override suspend fun execute(
        command: String,
        timeoutMillis: Long,
    ): SshCommandResult = withContext(Dispatchers.IO) {
        client.startSession().use { session ->
            session.exec(command).use { remoteCommand ->
                remoteCommand.join(timeoutMillis, TimeUnit.MILLISECONDS)
                SshCommandResult(
                    command = command,
                    exitStatus = remoteCommand.exitStatus ?: UNKNOWN_EXIT_STATUS,
                    stdout = remoteCommand.inputStream.readUtf8(),
                    stderr = remoteCommand.errorStream.readUtf8(),
                )
            }
        }
    }

    override suspend fun openPty(request: SshPtyRequest): SshPtyChannel {
        return withContext(Dispatchers.IO) {
            val session = client.startSession()
            session.allocatePTY(
                request.term,
                request.columns,
                request.rows,
                request.widthPixels,
                request.heightPixels,
                emptyMap(),
            )
            val shell = session.startShell()
            SshPtyChannel(
                input = shell.inputStream,
                output = shell.outputStream,
                resize = { size ->
                    withContext(Dispatchers.IO) {
                        shell.changeWindowDimensions(
                            size.columns,
                            size.rows,
                            size.widthPixels,
                            size.heightPixels,
                        )
                    }
                },
                close = {
                    withContext(Dispatchers.IO) {
                        runCatching { shell.close() }
                        session.close()
                    }
                },
            )
        }
    }

    private fun keyProviderFromPem(credential: SshAuthCredential.PrivateKey): OpenSSHKeyFile {
        val keyFile = OpenSSHKeyFile()
        val passphrase = credential.passphrase?.toCharArray()
        val passwordFinder = passphrase?.let { PasswordUtils.createOneOff(it) }
        keyFile.init(StringReader(credential.privateKeyPem), passwordFinder)
        return keyFile
    }

    private fun java.io.InputStream.readUtf8(): String {
        return readBytes().toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val UNKNOWN_EXIT_STATUS = -1
    }
}
