package dev.zakyymuh.timebox.core.plants

/**
 * The 18:00 check posts one notification per plant that needs water (due today or late,
 * not snoozed), each with its own Watered / Still moist buttons. Every plant gets its own
 * notification id and request codes, so one plant's buttons never act on another.
 */
object PlantNotices {

    data class Notice(val plantId: Long, val title: String)

    /** Late plants first, as on the plant page. */
    fun evening(overview: PlantsOverview): List<Notice> = overview.due.map { row ->
        val late = (row.status as PlantStatus.Due).daysLate
        val title = "${row.plant.name} needs water"
        Notice(row.plant.id, if (late == 0) title else "$title · ${PlantCopy.whenLabel(row.status)}")
    }

    /** Above every fixed id and code the app uses (2, 3, 200–203); four per plant. */
    private const val BASE = 10_000

    private fun slot(plantId: Long): Int = BASE + plantId.toInt() * 4

    fun notificationId(plantId: Long): Int = slot(plantId)
    fun openCode(plantId: Long): Int = slot(plantId) + 1
    fun waterCode(plantId: Long): Int = slot(plantId) + 2
    fun checkCode(plantId: Long): Int = slot(plantId) + 3
}
