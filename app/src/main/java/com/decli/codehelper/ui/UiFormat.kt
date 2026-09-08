package com.decli.codehelper.ui

import com.decli.codehelper.model.CodeFilterWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 时间范围文案，胶囊与面板共用同一套口径 */
fun rangeLabel(filter: CodeFilterWindow): String =
    if (filter == CodeFilterWindow.Last12Hours) {
        "最近 12 小时"
    } else {
        "最近 ${filter.hours / 24} 天"
    }

/** 短信时间口语化：今天 14:32 / 昨天 09:15 / 03-05 09:15 */
fun formatSmsTime(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone)
    val timeText = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (dateTime.toLocalDate()) {
        today -> "今天 $timeText"
        today.minusDays(1) -> "昨天 $timeText"
        else -> dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }
}
