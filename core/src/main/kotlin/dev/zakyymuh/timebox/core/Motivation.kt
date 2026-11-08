package dev.zakyymuh.timebox.core

import java.time.LocalDate

/** A quiet line for an empty day, one per date, cycling through the whole set. */
object Motivation {
    val lines = listOf(
        "Plan your day for a greater future.",
        "Small steps today, big change tomorrow.",
        "A planned hour saves a wasted day.",
        "Decide what matters, then begin.",
        "Start where you are. Do what you can.",
        "Today is a good day to grow.",
        "Focus on one thing, then the next.",
        "Little by little, a little becomes a lot.",
        "Write it down, then make it happen.",
        "Be kind to future you: plan today.",
    )

    fun forDay(date: LocalDate): String =
        lines[Math.floorMod(date.toEpochDay(), lines.size.toLong()).toInt()]
}
