package com.decli.codehelper.ui.home

import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRowsTest {

    private fun item(
        key: String,
        sender: String,
        codes: List<String>,
        isPickedUp: Boolean = false,
        receivedAtMillis: Long = 0L,
    ) = PickupCodeItem(
        uniqueKey = key,
        smsId = key.hashCode().toLong(),
        codes = codes,
        sender = sender,
        body = "",
        preview = "",
        receivedAtMillis = receivedAtMillis,
        matchedRules = emptyList(),
        isPickedUp = isPickedUp,
    )

    @Test
    fun `groups pending items by station and keeps input order`() {
        val rows = buildHomeRows(
            items = listOf(
                item("a", "菜鸟驿站", listOf("27017")),
                item("b", "兔喜快递", listOf("1145")),
                item("c", "菜鸟驿站", listOf("3-3-06006", "8-5-06003")),
            ),
            groupBySender = true,
        )

        assertEquals(
            listOf(
                "group:菜鸟驿站",
                "pending:a",
                "pending:c",
                "group:兔喜快递",
                "pending:b",
            ),
            rows.map { it.key },
        )
        val firstGroup = rows.first() as HomeRow.GroupHeader
        assertEquals(3, firstGroup.codeCount)
    }

    @Test
    fun `emits no headers when grouping is off`() {
        val rows = buildHomeRows(
            items = listOf(
                item("a", "菜鸟驿站", listOf("27017")),
                item("b", "兔喜快递", listOf("1145")),
            ),
            groupBySender = false,
        )

        assertEquals(listOf("pending:a", "pending:b"), rows.map { it.key })
    }

    @Test
    fun `picked up items always land in a trailing group`() {
        val rows = buildHomeRows(
            items = listOf(
                item("done", "菜鸟驿站", listOf("27017"), isPickedUp = true),
                item("todo", "兔喜快递", listOf("1145")),
            ),
            groupBySender = true,
        )

        assertEquals(
            listOf("group:兔喜快递", "pending:todo", "group:$PICKED_UP_GROUP_TITLE", "picked:done"),
            rows.map { it.key },
        )
    }

    @Test
    fun `picked up items are summarised even without grouping`() {
        val rows = buildHomeRows(
            items = listOf(item("done", "菜鸟驿站", listOf("27017"), isPickedUp = true)),
            groupBySender = false,
        )

        assertTrue(rows.first() is HomeRow.GroupHeader)
        assertTrue(rows.last() is HomeRow.PickedUp)
    }

    @Test
    fun `counts pending codes per time window`() {
        val now = 1_000_000_000_000L
        val hour = 60L * 60L * 1000L
        val counts = pendingCountByWindow(
            pendingItems = listOf(
                item("recent", "菜鸟驿站", listOf("1", "2"), receivedAtMillis = now - 2 * hour),
                item("twoDays", "兔喜快递", listOf("3"), receivedAtMillis = now - 40 * hour),
                item("tenDays", "中通快递", listOf("4"), receivedAtMillis = now - 240 * hour),
            ),
            nowMillis = now,
        )

        assertEquals(2, counts[CodeFilterWindow.Last12Hours])
        assertEquals(2, counts[CodeFilterWindow.Last1Day])
        assertEquals(3, counts[CodeFilterWindow.Last3Days])
        assertEquals(3, counts[CodeFilterWindow.Last7Days])
        assertEquals(4, counts[CodeFilterWindow.Last14Days])
    }
}
