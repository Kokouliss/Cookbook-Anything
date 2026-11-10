package com.cookbookanything.core.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The days of [month] laid out in full weeks that start on [firstDayOfWeek].
 * Null cells pad the first and last week.
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = List<LocalDate?>(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val trailing = (7 - days.size % 7) % 7
    return days + List<LocalDate?>(trailing) { null }
}
