package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.cooking.timerMinutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StepTimerTest {
    @Test
    fun `reads minutes in english and greek`() {
        assertEquals(20, timerMinutes("Bake at 180°C for 20 minutes."))
        assertEquals(20, timerMinutes("Ψήνουμε στους 180 βαθμούς για 20 λεπτά."))
        assertEquals(45, timerMinutes("Σιγοβράζουμε 45' ανακατεύοντας."))
        assertEquals(5, timerMinutes("Rest 5 min"))
    }

    @Test
    fun `hours and ranges`() {
        assertEquals(90, timerMinutes("Leave to rise for 1,5 ώρες."))
        assertEquals(75, timerMinutes("Roast for 1 hour and 15 minutes."))
        assertEquals(20, timerMinutes("Simmer for 20-25 minutes until thick."))
        assertEquals(60, timerMinutes("Chill 1 hour. Then 10 minutes at room temperature."))
    }

    @Test
    fun `no duration no timer`() {
        assertNull(timerMinutes("Chop 3 onions and 180 g of butter."))
        assertNull(timerMinutes("Stir for 30 seconds."))
    }
}
