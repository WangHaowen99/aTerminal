package com.aterminal.app.tmux

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellQuoterTest {
    @Test
    fun quotesEmptyString() {
        assertEquals("''", ShellQuoter.quote(""))
    }

    @Test
    fun quotesSimpleWordsAndSpaces() {
        assertEquals("'codex'", ShellQuoter.quote("codex"))
        assertEquals("'codex resume --last'", ShellQuoter.quote("codex resume --last"))
    }

    @Test
    fun quotesSingleQuotesUsingPosixSafeSequence() {
        assertEquals("'it'\"'\"'s safe'", ShellQuoter.quote("it's safe"))
    }

    @Test
    fun quotesColonsAndPathLikeValues() {
        assertEquals("'aterm:codex:backend-api'", ShellQuoter.quote("aterm:codex:backend-api"))
        assertEquals("'/srv/apps/aTerminal mobile'", ShellQuoter.quote("/srv/apps/aTerminal mobile"))
    }
}
