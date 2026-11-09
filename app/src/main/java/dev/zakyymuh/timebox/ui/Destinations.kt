package dev.zakyymuh.timebox.ui

import android.content.Context
import android.content.Intent

/** Every screen's route, and how notifications ask for one. */
object Destinations {
    const val DASHBOARD = "dashboard"
    const val PLANTS = "plants"
    const val CALENDAR = "plants/calendar"
    const val EDIT = "plants/edit/{id}"
    const val ENCYCLOPEDIA = "encyclopedia"
    const val ENTRY = "encyclopedia/{id}"
    const val SOIL = "soil"
    const val COLLECTION = "collection"
    const val HISTORY = "history"
    const val DEX = "collection/{id}"
    const val DEX_EDIT = "collection/edit/{id}/{owned}"

    /** Route to one plant in the collection or wishlist. */
    fun dex(id: Long): String = "collection/$id"

    /** Route to add ([id] 0; [owned] picks collection or wishlist) or edit an entry. */
    fun dexEdit(id: Long, owned: Boolean): String = "collection/edit/$id/$owned"

    /** Route to one encyclopedia entry. */
    fun entry(id: String): String = "encyclopedia/$id"

    /** Route to edit plant [id]; 0 means a new plant. */
    fun edit(id: Long): String = "plants/edit/$id"

    const val EXTRA = "destination"

    /**
     * Opens the app on [destination] with the dashboard underneath, so Back always lands
     * on the dashboard. NEW_TASK | CLEAR_TASK makes onCreate run every time, which is
     * where the destination is read.
     */
    fun intent(context: Context, destination: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA, destination)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    /** The screen an intent asks for, or null to stay on the dashboard. */
    fun from(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA)?.takeIf { it == PLANTS || it == SOIL }
}
