package com.decli.codehelper.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PickupCodeItemTest {

    @Test
    fun `keeps short sender as is`() {
        assertEquals("菜鸟驿站", PickupCodeItem.shortenSender("菜鸟驿站"))
        assertEquals("兔喜快递", PickupCodeItem.shortenSender("兔喜快递"))
    }

    @Test
    fun `strips brackets and takes first segment`() {
        assertEquals("菜鸟驿站", PickupCodeItem.shortenSender("【菜鸟驿站】"))
        assertEquals("中通快递", PickupCodeItem.shortenSender("中通快递 · 望京站"))
        assertEquals("圆通速递", PickupCodeItem.shortenSender("圆通速递，望京站"))
    }

    @Test
    fun `truncates overlong sender to six characters`() {
        assertEquals("超长快递名字", PickupCodeItem.shortenSender("超长快递名字还没结束"))
    }

    @Test
    fun `keeps number senders complete so they are never misread`() {
        assertEquals("10655021111", PickupCodeItem.shortenSender("10655021111"))
        assertEquals("+8613800138000", PickupCodeItem.shortenSender("+8613800138000"))
    }

    @Test
    fun `falls back to a readable label for blank senders`() {
        assertEquals("短信", PickupCodeItem.shortenSender("   "))
    }
}
