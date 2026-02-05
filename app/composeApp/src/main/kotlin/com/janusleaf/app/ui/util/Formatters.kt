package com.janusleaf.app.ui.util

import android.text.format.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val entryDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val axisMonthFormatter = DateTimeFormatter.ofPattern("MMM")
private val axisWeekFormatter = DateTimeFormatter.ofPattern("EEE")
private val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

fun formatEntryDate(date: LocalDate): String = entryDateFormatter.format(date)

fun formatHeaderDate(date: LocalDate): String = headerDateFormatter.format(date)

fun formatShortDate(date: LocalDate): String = shortDateFormatter.format(date)

fun formatAxisDate(date: LocalDate, isWeek: Boolean): String {
    return if (isWeek) axisWeekFormatter.format(date) else axisMonthFormatter.format(date)
}

fun formatRelativeTime(instant: Instant): String {
    val epochMillis = instant.toEpochMilli()
    return DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

fun stripMarkdown(text: String): String {
    var result = text
    result = result.replace("## ", "").replace("# ", "")
    result = result.replace("**", "")
    result = result.replace("<u>", "").replace("</u>", "")
    result = result.replace("~~", "")
    val italicRegex = Regex("(?<!\\*)\\*(?!\\*)")
    result = result.replace(italicRegex, "")
    result = result.replace(Regex("\\n\\n+"), " ")
    result = result.replace("\n", " ")
    result = result.replace(Regex("  +"), " ")
    return result.trim()
}

fun Instant.toLocalDate(): LocalDate {
    return this.atZone(ZoneId.systemDefault()).toLocalDate()
}
