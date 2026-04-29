package com.aterminal.app.readability

class AgentOutputParser(
    private val longOutputLineThreshold: Int = DEFAULT_LONG_OUTPUT_LINE_THRESHOLD,
) {
    fun parse(raw: String): List<AgentOutputBlock> {
        if (raw.isBlank()) {
            return emptyList()
        }

        val sanitized = AnsiSanitizer.sanitize(raw)
        if (sanitized.isBlank()) {
            return listOf(AgentOutputBlock.Unknown(rawSource = raw, text = sanitized))
        }

        return segment(sanitized).map { block ->
            classify(block)
        }
    }

    private fun segment(text: String): List<String> {
        val lines = text.lines()
        val blocks = mutableListOf<String>()
        val current = mutableListOf<String>()
        var inFence = false

        fun flush() {
            if (current.isNotEmpty()) {
                blocks += current.joinToString(separator = "\n").trim()
                current.clear()
            }
        }

        for (line in lines) {
            if (line.trimStart().startsWith(CODE_FENCE)) {
                current += line
                inFence = !inFence
                if (!inFence) {
                    flush()
                }
                continue
            }

            if (!inFence && line.isBlank()) {
                flush()
                continue
            }

            current += line
        }
        flush()

        return blocks.filter { it.isNotBlank() }
    }

    private fun classify(block: String): AgentOutputBlock {
        parseCode(block)?.let { return it }
        if (isDiff(block)) {
            return AgentOutputBlock.Diff(rawSource = block, text = block)
        }
        if (isApproval(block)) {
            return AgentOutputBlock.Approval(rawSource = block, text = block)
        }
        parseHeading(block)?.let { return it }
        parseList(block)?.let { return it }
        parseCommand(block)?.let { return it }
        if (block.lines().size >= longOutputLineThreshold) {
            return AgentOutputBlock.Output(
                rawSource = block,
                text = block,
                collapsedByDefault = true,
            )
        }
        return AgentOutputBlock.PlainText(rawSource = block, text = block)
    }

    private fun parseCode(block: String): AgentOutputBlock.Code? {
        val lines = block.lines()
        val firstLine = lines.firstOrNull()?.trim() ?: return null
        val lastLine = lines.lastOrNull()?.trim() ?: return null
        if (!firstLine.startsWith(CODE_FENCE) || lastLine != CODE_FENCE) {
            return null
        }
        val language = firstLine.removePrefix(CODE_FENCE).trim().ifBlank { null }
        return AgentOutputBlock.Code(
            rawSource = block,
            language = language,
            code = lines.drop(1).dropLast(1).joinToString(separator = "\n"),
        )
    }

    private fun parseHeading(block: String): AgentOutputBlock.Heading? {
        val match = HEADING.matchEntire(block.trim()) ?: return null
        return AgentOutputBlock.Heading(
            rawSource = block,
            level = match.groupValues[1].length,
            text = match.groupValues[2].trim(),
        )
    }

    private fun parseList(block: String): AgentOutputBlock.ListBlock? {
        val lines = block.lines()
        if (lines.isEmpty() || !lines.all { LIST_ITEM.matches(it.trim()) }) {
            return null
        }
        return AgentOutputBlock.ListBlock(
            rawSource = block,
            items = lines.map {
                it.trim().replace(LIST_MARKER, "")
            },
        )
    }

    private fun parseCommand(block: String): AgentOutputBlock.Command? {
        val match = COMMAND.matchEntire(block.trim()) ?: return null
        return AgentOutputBlock.Command(
            rawSource = block,
            command = match.groupValues[1].trim(),
        )
    }

    private fun isDiff(block: String): Boolean {
        val lines = block.lines()
        return lines.firstOrNull()?.startsWith("diff --git") == true ||
            lines.any { it.startsWith("@@ ") } ||
            lines.any { it.startsWith("--- ") } && lines.any { it.startsWith("+++ ") }
    }

    private fun isApproval(block: String): Boolean {
        return APPROVAL_HINTS.any { it.containsMatchIn(block) }
    }

    private companion object {
        const val CODE_FENCE = "```"
        const val DEFAULT_LONG_OUTPUT_LINE_THRESHOLD = 10
        val HEADING = Regex("(#{1,6})\\s+(.+)")
        val LIST_ITEM = Regex("""(?:[-*]|\d+\.)\s+.+""")
        val LIST_MARKER = Regex("""^(?:[-*]|\d+\.)\s+""")
        val COMMAND = Regex("""`?\$\s+(.+?)`?""")
        val APPROVAL_HINTS = listOf(
            Regex("(?i)approve\\?"),
            Regex("(?i)permission"),
            Regex("(?i)wants to run"),
        )
    }
}
