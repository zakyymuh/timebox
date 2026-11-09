package dev.zakyymuh.timebox.core.motor

/** What kind of thing you ride or drive. Stored by name, so never rename an entry. */
enum class VehicleType(val label: String) {
    MOTORCYCLE("Motorcycle"),
    CAR("Car"),
    OTHER("Other"),
}

/** One of your vehicles. [photo] is the file name of its picture in app storage, if any. */
data class Vehicle(
    val id: Long,
    val type: VehicleType,
    val name: String,
    val plate: String,
    val photo: String?,
) {
    /** "Vario · B 1234 XYZ", or just the name when there is no plate. */
    val title: String get() = if (plate.isEmpty()) name else "$name · $plate"
}

/** What the vehicle edit screen holds. */
data class VehicleForm(
    val type: VehicleType = VehicleType.MOTORCYCLE,
    val name: String = "",
    val plate: String = "",
    val photo: String? = null,
) {
    fun isValid(): Boolean =
        name.isNotBlank() && name.trim().length <= MAX_NAME && Plate.normalise(plate).length <= MAX_PLATE

    companion object {
        const val MAX_NAME = 40
        const val MAX_PLATE = 12
    }
}

/** Indonesian plates (plat nomor) read "B 1234 XYZ": capitals, single spaces. */
object Plate {
    private val spaces = Regex("\\s+")

    fun normalise(raw: String): String = raw.trim().uppercase().replace(spaces, " ")
}
