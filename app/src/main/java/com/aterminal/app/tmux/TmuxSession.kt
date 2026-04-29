package com.aterminal.app.tmux

data class TmuxSession(
    val name: String,
    val createdAtEpochSeconds: Long,
    val attached: Boolean,
    val windowCount: Int,
)

object TmuxSessionParser {
    fun parseListSessions(output: String): List<TmuxSession> {
        return output
            .lineSequence()
            .mapNotNull(::parseLine)
            .toList()
    }

    private fun parseLine(line: String): TmuxSession? {
        if (line.isBlank()) {
            return null
        }
        val parts = line.split('\t')
        if (parts.size != FIELD_COUNT) {
            return null
        }
        val createdAt = parts[1].toLongOrNull() ?: return null
        val windowCount = parts[3].toIntOrNull() ?: return null
        return TmuxSession(
            name = parts[0],
            createdAtEpochSeconds = createdAt,
            attached = parts[2] == "1",
            windowCount = windowCount,
        )
    }

    private const val FIELD_COUNT = 4
}
