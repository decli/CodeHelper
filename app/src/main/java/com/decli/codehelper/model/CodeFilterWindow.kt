package com.decli.codehelper.model

enum class CodeFilterWindow(
    val label: String,
    val hours: Long,
) {
    Last12Hours(label = "12小时", hours = 12),
    Last1Day(label = "1天内", hours = 24),
    Last2Days(label = "2天内", hours = 48),
    Last5Days(label = "5天内", hours = 120),
}

