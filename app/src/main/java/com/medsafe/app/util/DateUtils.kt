package com.medsafe.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object DateUtils {
    fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Exclusive upper bound. */
    fun startOfNextDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun instantOf(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Instant =
        LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, minute).atZone(zone).toInstant()

    fun toDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
}
