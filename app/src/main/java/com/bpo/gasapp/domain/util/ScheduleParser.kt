package com.bpo.gasapp.domain.util

import java.util.Calendar

/**
 * Parses the official API schedule strings (e.g. "L-D: 24H",
 * "L-V: 06:00-22:00; S: 08:00-14:00") and decides whether a station is open
 * at a given instant. Returns null when the format is unknown, so callers can
 * treat "unknown" as "do not hide".
 */
object ScheduleParser {

    // L M X J V S D -> Calendar day-of-week (Sunday = 1)
    private val dayIndex = mapOf(
        'L' to Calendar.MONDAY,
        'M' to Calendar.TUESDAY,
        'X' to Calendar.WEDNESDAY,
        'J' to Calendar.THURSDAY,
        'V' to Calendar.FRIDAY,
        'S' to Calendar.SATURDAY,
        'D' to Calendar.SUNDAY
    )
    private val order = listOf('L', 'M', 'X', 'J', 'V', 'S', 'D')

    fun isOpen(schedule: String, calendar: Calendar = Calendar.getInstance()): Boolean? {
        val raw = schedule.trim()
        if (raw.isEmpty()) return null

        val today = calendar.get(Calendar.DAY_OF_WEEK)
        val minutesNow = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        var parsedAny = false
        raw.split(';').forEach { segment ->
            val parts = segment.split(':', limit = 2)
            if (parts.size != 2) return@forEach
            val days = parseDays(parts[0]) ?: return@forEach
            if (today !in days) {
                parsedAny = true
                return@forEach
            }
            val timeSpec = parts[1].trim()
            parsedAny = true
            if (timeSpec.equals("24H", ignoreCase = true)) return true
            if (isWithinAnyInterval(timeSpec, minutesNow)) return true
        }
        return if (parsedAny) false else null
    }

    private fun parseDays(spec: String): Set<Int>? {
        val s = spec.trim().uppercase()
        return when {
            s.contains('-') -> {
                val (a, b) = s.split('-', limit = 2).map { it.trim().firstOrNull() }
                if (a == null || b == null) return null
                val start = order.indexOf(a)
                val end = order.indexOf(b)
                if (start == -1 || end == -1 || start > end) return null
                order.subList(start, end + 1).mapNotNull { dayIndex[it] }.toSet()
            }
            else -> s.mapNotNull { dayIndex[it] }.toSet().takeIf { it.isNotEmpty() }
        }
    }

    private fun isWithinAnyInterval(timeSpec: String, minutesNow: Int): Boolean {
        // Intervals may be separated by " Y " (e.g. "07:00-14:00 Y 16:00-22:00")
        return timeSpec.split(" Y ", " y ").any { interval ->
            val bounds = interval.trim().split('-')
            if (bounds.size != 2) return@any false
            val open = bounds[0].toMinutesOrNull() ?: return@any false
            val close = bounds[1].toMinutesOrNull() ?: return@any false
            if (close >= open) minutesNow in open..close
            else minutesNow >= open || minutesNow <= close // crosses midnight
        }
    }

    private fun String.toMinutesOrNull(): Int? {
        val hm = trim().split(':')
        if (hm.size != 2) return null
        val h = hm[0].toIntOrNull() ?: return null
        val m = hm[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
