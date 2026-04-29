package com.aterminal.app.readability

sealed class AgentOutputBlock {
    abstract val rawSource: String

    data class PlainText(
        override val rawSource: String,
        val text: String,
    ) : AgentOutputBlock()

    data class Heading(
        override val rawSource: String,
        val level: Int,
        val text: String,
    ) : AgentOutputBlock()

    data class ListBlock(
        override val rawSource: String,
        val items: List<String>,
    ) : AgentOutputBlock()

    data class Code(
        override val rawSource: String,
        val language: String?,
        val code: String,
    ) : AgentOutputBlock()

    data class Command(
        override val rawSource: String,
        val command: String,
    ) : AgentOutputBlock()

    data class Output(
        override val rawSource: String,
        val text: String,
        val collapsedByDefault: Boolean,
    ) : AgentOutputBlock()

    data class Diff(
        override val rawSource: String,
        val text: String,
    ) : AgentOutputBlock()

    data class Approval(
        override val rawSource: String,
        val text: String,
    ) : AgentOutputBlock()

    data class Unknown(
        override val rawSource: String,
        val text: String,
    ) : AgentOutputBlock()
}
