package com.aterminal.app.tmux

object TmuxSessionName {
    fun forWorkspace(
        agent: String,
        workspaceName: String,
        suffix: String? = null,
    ): String {
        val segments = mutableListOf(
            PREFIX,
            slugify(agent),
            slugify(workspaceName),
        )
        suffix?.takeIf { it.isNotBlank() }?.let {
            segments += slugify(it)
        }
        return segments.joinToString(separator = ":")
    }

    private fun slugify(value: String): String {
        val slug = value
            .replace(CAMEL_CASE_BOUNDARY, "$1-$2")
            .lowercase()
            .replace(NON_ASCII_LETTERS_OR_DIGITS, "-")
            .trim('-')
            .replace(REPEATED_DASHES, "-")
        return slug.ifBlank { FALLBACK_WORKSPACE_SLUG }
    }

    private const val FALLBACK_WORKSPACE_SLUG = "workspace"
    private const val PREFIX = "aterm"
    private val CAMEL_CASE_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    private val NON_ASCII_LETTERS_OR_DIGITS = Regex("[^a-z0-9]+")
    private val REPEATED_DASHES = Regex("-+")
}
