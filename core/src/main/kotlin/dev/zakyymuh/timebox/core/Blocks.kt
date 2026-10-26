package dev.zakyymuh.timebox.core

import java.time.LocalDate

/** Operations on the shape of a plan, as opposed to its progress. */
object Blocks {

    /**
     * Moves the block at [fromIndex] to [toIndex] and renumbers, so positions stay
     * contiguous from zero and the list order is always the day order.
     */
    fun reorder(blocks: List<Block>, fromIndex: Int, toIndex: Int): List<Block> {
        if (fromIndex == toIndex) return blocks
        val working = blocks.toMutableList()
        working.add(toIndex, working.removeAt(fromIndex))
        return renumber(working)
    }

    /** Closes gaps left by deleting or moving, keeping the current list order. */
    fun renumber(blocks: List<Block>): List<Block> =
        blocks.mapIndexed { index, block -> block.copy(position = index) }

    /**
     * Copies a previous day's blocks as a fresh plan for [newDate].
     *
     * Everything that records history is dropped — timestamps, the skipped flag, and the
     * ids, so the copies are saved as new rows rather than overwriting the day they came
     * from. The old intention goes too: today's vision is today's to write.
     */
    fun templateFrom(day: DayTimebox, newDate: LocalDate): DayTimebox =
        DayTimebox(
            date = newDate,
            intention = null,
            blocks = renumber(
                day.blocks.map {
                    Block(
                        id = 0L,
                        position = it.position,
                        title = it.title,
                        planned = it.planned,
                    )
                },
            ),
        )
}
