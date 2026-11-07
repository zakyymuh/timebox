package dev.zakyymuh.timebox.nudge

import dev.zakyymuh.timebox.data.PlantRepository

/** What the 6pm notification's buttons do, kept apart from the receiver so it can be tested. */
object PlantActions {
    const val ACTION_WATER = "dev.zakyymuh.timebox.WATER"
    const val ACTION_CHECK = "dev.zakyymuh.timebox.CHECK"
    const val EXTRA_PLANT = "plant"

    /** Returns whether anything was done. A plant deleted meanwhile is simply ignored. */
    suspend fun handle(repo: PlantRepository, action: String?, plantId: Long): Boolean {
        if (repo.plant(plantId) == null) return false
        return when (action) {
            ACTION_WATER -> {
                repo.water(plantId)
                true
            }
            ACTION_CHECK -> {
                repo.snooze(plantId)
                true
            }
            else -> false
        }
    }
}
