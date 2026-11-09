package com.cookbookanything.core.domain

import com.cookbookanything.core.domain.calendar.monthGrid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonthGridTest {
    @Test
    fun `november 2026 starts on sunday so a monday first week has six blanks`() {
        val grid = monthGrid(YearMonth.of(2026, 11), DayOfWeek.MONDAY)

        assertEquals(42, grid.size)
        assertEquals(List(6) { null }, grid.take(6))
        assertEquals(LocalDate.of(2026, 11, 1), grid[6])
        assertEquals(LocalDate.of(2026, 11, 30), grid[35])
        assertNull(grid.last())
    }

    @Test
    fun `a month that fills its weeks exactly has no padding`() {
        // February 2026 starts on a Sunday and has 28 days.
        val grid = monthGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)

        assertEquals(28, grid.size)
        assertEquals(LocalDate.of(2026, 2, 1), grid.first())
        assertEquals(LocalDate.of(2026, 2, 28), grid.last())
    }
}
