package dev.zakyymuh.timebox.core

import java.time.Duration

/** How hard it is to earn a pass, and how long one lasts. */
data class PassPolicy(
    val minReasonChars: Int = 15,
    val minWords: Int = 3,
    val minDistinctChars: Int = 6,
    val waitSeconds: Int = 10,
    val passDuration: Duration = Duration.ofMinutes(5),
) {
    companion object {
        val DEFAULT = PassPolicy()
    }
}

/**
 * Decides whether a typed reason is a reason or a formality.
 *
 * A plain length check is not enough: fifteen spaces and "aaaaaaaaaaaaaaaa" both clear
 * one, and a gate that accepts them has stopped asking anything. So a reason has to be
 * long enough, made of several real words, and varied enough to be typed rather than held
 * down.
 */
class ReasonValidator {

    fun isAcceptable(raw: String, policy: PassPolicy): Boolean {
        val trimmed = raw.trim()
        if (trimmed.length < policy.minReasonChars) return false

        val realWords = trimmed.split(WHITESPACE).count { it.length >= 2 }
        if (realWords < policy.minWords) return false

        val distinct = trimmed.filterNot { it.isWhitespace() }.toSet().size
        return distinct >= policy.minDistinctChars
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
