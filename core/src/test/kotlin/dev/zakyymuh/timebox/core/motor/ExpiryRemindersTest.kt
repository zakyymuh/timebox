package dev.zakyymuh.timebox.core.motor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpiryRemindersTest {
    private val today = LocalDate.of(2026, 3, 5)

    private fun simC(daysLeft: Long, id: Long = 1) =
        Expiry(id, vehicleId = null, name = "SIM C", date = today.plusDays(daysLeft), renewMonths = 60)

    private fun due(vararg expiries: Expiry, sent: Set<SentReminder> = emptySet()) =
        ExpiryReminders.due(expiries.toList(), emptyMap(), today, sent)

    @Test
    fun `a week before, it says so`() {
        val r = due(simC(7)).single()
        assertEquals(ReminderStage.WEEK, r.stage)
        assertEquals("SIM C expires in 1 week, 12 Mar", r.title)
        assertEquals("Renew it before 12 Mar 2026.", r.text)
    }

    @Test
    fun `a month before, it says so`() {
        val r = due(simC(30)).single()
        assertEquals(ReminderStage.MONTH, r.stage)
        assertEquals("SIM C expires in 1 month, 4 Apr", r.title)
    }

    @Test
    fun `nothing further out than 30 days, and nothing once it has passed`() {
        assertTrue(due(simC(31)).isEmpty())
        assertTrue(due(simC(-1)).isEmpty())
    }

    @Test
    fun `a missed day still warns, with the real count`() {
        assertEquals("SIM C expires in 28 days, 2 Apr", due(simC(28)).single().title)
        assertEquals("SIM C expires tomorrow, 6 Mar", due(simC(1)).single().title)
        assertEquals("SIM C expires today, 5 Mar", due(simC(0)).single().title)
    }

    @Test
    fun `the same reminder never goes out twice`() {
        val month = simC(20)
        val sent = setOf(SentReminder(1, month.date, "30d"))
        assertTrue(due(month, sent = sent).isEmpty())
        // The week warning is its own reminder.
        val week = simC(6)
        assertEquals(ReminderStage.WEEK, due(week, sent = setOf(SentReminder(1, week.date, "30d"))).single().stage)
        assertTrue(due(week, sent = setOf(SentReminder(1, week.date, "7d"))).isEmpty())
    }

    @Test
    fun `renewing starts a fresh pair, since the date moved`() {
        val renewed = simC(5)
        val sentForOldDate = setOf(SentReminder(1, renewed.date.minusYears(5), "7d"))
        assertEquals(1, due(renewed, sent = sentForOldDate).size)
    }

    @Test
    fun `a vehicle's date carries the vehicle's name`() {
        val stnk = Expiry(2, vehicleId = 9, name = "STNK tax", date = today.plusDays(7), renewMonths = 12)
        val r = ExpiryReminders.due(listOf(stnk), mapOf(2L to "STNK tax Avanza"), today, emptySet()).single()
        assertEquals("STNK tax Avanza expires in 1 week, 12 Mar", r.title)
        assertEquals(SentReminder(2, stnk.date, "7d"), r.sent)
    }
}
