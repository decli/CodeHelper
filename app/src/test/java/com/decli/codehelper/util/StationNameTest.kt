package com.decli.codehelper.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationNameTest {

    // 真机上的四条菜鸟驿站短信：三条带签名、一条（5G 消息）不带
    private val twoParcels =
        "【菜鸟驿站】您有2个包裹在龙腾苑四区兔喜生活30号楼店，取件码为17-7-24040,4-3-24011"
    private val oneParcel =
        "【菜鸟驿站】您的包裹已到站，凭6-4-12008到龙腾苑四区兔喜生活30号楼店取件。"
    private val unsigned =
        "您的包裹已到站，凭15-2-05024到龙腾苑四区兔喜生活30号楼店取件。"

    @Test
    fun `reads the leading signature instead of the platform number`() {
        assertEquals("菜鸟驿站", StationName.resolve(twoParcels, address = "1068474310000003825"))
        assertEquals("菜鸟驿站", StationName.resolve(oneParcel, address = "106830250000205"))
    }

    @Test
    fun `reads a trailing signature`() {
        assertEquals(
            "丰巢",
            StationName.fromSignature("您的快递已存入丰巢柜，取件码 123456。【丰巢】"),
        )
    }

    @Test
    fun `keeps only the brand when the signature carries a branch in parentheses`() {
        assertEquals("菜鸟驿站", StationName.fromSignature("【菜鸟驿站（龙腾苑店）】您的包裹已到站"))
        assertEquals("菜鸟驿站", StationName.fromSignature("【菜鸟驿站(龙腾苑店)】您的包裹已到站"))
    }

    @Test
    fun `ignores brackets in the middle of the body and code-like brackets`() {
        assertNull(StationName.fromSignature("您的包裹已到站，凭【6-4-12008】取件"))
        assertNull(StationName.fromSignature("【6-4-12008】到龙腾苑取件"))
    }

    @Test
    fun `uses the sender when it is already a name`() {
        assertEquals("菜鸟驿站", StationName.resolve(unsigned, address = "菜鸟驿站"))
        assertNull(StationName.fromAddress("106836455068"))
        assertNull(StationName.fromAddress("+8613800138000"))
        assertNull(StationName.fromAddress("sip:106836455068@botplatform.rcs.example.cn"))
    }

    @Test
    fun `falls back to the same sender's earlier signature`() {
        assertEquals(
            "菜鸟驿站",
            StationName.resolve(unsigned, address = "106836455068") { "菜鸟驿站" },
        )
    }

    @Test
    fun `falls back to a brand named in the body`() {
        assertEquals("兔喜生活", StationName.resolve(unsigned, address = "106836455068"))
        assertEquals("菜鸟驿站", StationName.fromBrandKeyword("请到菜鸟驿站龙腾苑店取件"))
        assertEquals("菜鸟驿站", StationName.fromBrandKeyword("请到菜鸟兔喜生活店取件"))
    }

    @Test
    fun `does not query the sender history when the body already has a signature`() {
        var queried = false
        StationName.resolve(twoParcels, address = "1068474310000003825") {
            queried = true
            null
        }
        assertEquals(false, queried)
    }

    @Test
    fun `returns blank when nothing identifies the station`() {
        assertEquals("", StationName.resolve("取件码 27017", address = "10690000"))
    }
}
