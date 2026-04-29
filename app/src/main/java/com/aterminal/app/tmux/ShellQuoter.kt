package com.aterminal.app.tmux

object ShellQuoter {
    fun quote(value: String): String {
        if (value.isEmpty()) {
            return "''"
        }
        return "'${value.replace("'", "'\"'\"'")}'"
    }
}
