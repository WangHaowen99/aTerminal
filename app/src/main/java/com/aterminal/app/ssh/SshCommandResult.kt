package com.aterminal.app.ssh

data class SshCommandResult(
    val command: String,
    val exitStatus: Int,
    val stdout: String,
    val stderr: String,
)
