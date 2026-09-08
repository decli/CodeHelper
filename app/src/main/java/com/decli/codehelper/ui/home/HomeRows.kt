package com.decli.codehelper.ui.home

import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem

/** 首页列表的一行：分组标题 / 待取卡 / 已取件摘要行 */
sealed interface HomeRow {
    val key: String

    data class GroupHeader(
        val title: String,
        val codeCount: Int,
    ) : HomeRow {
        override val key: String get() = "group:$title"
    }

    data class Pending(
        val item: PickupCodeItem,
    ) : HomeRow {
        override val key: String get() = "pending:${item.uniqueKey}"
    }

    data class PickedUp(
        val item: PickupCodeItem,
    ) : HomeRow {
        override val key: String get() = "picked:${item.uniqueKey}"
    }
}

const val PICKED_UP_GROUP_TITLE = "已取件"

/**
 * 把列表数据摊平成可直接渲染的行。
 *
 * - 待取项按驿站分组（保持原有的时间倒序），分组开关关闭时不产生标题；
 * - 已取件只在「全部包裹」里出现，统一放到末尾的「已取件」组，并降为一行摘要。
 */
fun buildHomeRows(
    items: List<PickupCodeItem>,
    groupBySender: Boolean,
): List<HomeRow> {
    val rows = mutableListOf<HomeRow>()
    val pending = items.filter { !it.isPickedUp }
    val pickedUp = items.filter { it.isPickedUp }

    if (groupBySender) {
        pending
            .groupBy { it.senderShort }
            .forEach { (sender, group) ->
                rows += HomeRow.GroupHeader(
                    title = sender,
                    codeCount = group.sumOf { it.codeCount },
                )
                group.forEach { rows += HomeRow.Pending(it) }
            }
    } else {
        pending.forEach { rows += HomeRow.Pending(it) }
    }

    if (pickedUp.isNotEmpty()) {
        rows += HomeRow.GroupHeader(
            title = PICKED_UP_GROUP_TITLE,
            codeCount = pickedUp.sumOf { it.codeCount },
        )
        pickedUp.forEach { rows += HomeRow.PickedUp(it) }
    }

    return rows
}

/**
 * 时间面板的「N 件待取」预览：对最近 14 天的待取数据本地统计，
 * 每一档只统计落在该时间窗内的取件码条数。
 */
fun pendingCountByWindow(
    pendingItems: List<PickupCodeItem>,
    nowMillis: Long,
): Map<CodeFilterWindow, Int> =
    CodeFilterWindow.entries.associateWith { window ->
        val sinceMillis = nowMillis - window.hours * 60L * 60L * 1000L
        pendingItems
            .filter { !it.isPickedUp && it.receivedAtMillis >= sinceMillis }
            .sumOf { it.codeCount }
    }
