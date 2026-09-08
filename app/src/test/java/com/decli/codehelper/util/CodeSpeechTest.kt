package com.decli.codehelper.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeSpeechTest {

    @Test
    fun `spells a code digit by digit and reads the hyphen`() {
        assertEquals("3 杠 3 杠 0 6 0 0 6", CodeSpeech.spellCode("3-3-06006"))
    }

    @Test
    fun `spells letters as letters`() {
        assertEquals("B 杠 2 杠 1 1 8 8", CodeSpeech.spellCode("B-2-1188"))
    }

    @Test
    fun `separates multiple codes with a spoken hint`() {
        assertEquals(
            "2 7 0 1 7，下一个，1 1 4 5",
            CodeSpeech.spellCodes(listOf("27017", "1145")),
        )
    }

    @Test
    fun `speech text starts with the station name`() {
        assertEquals(
            "菜鸟驿站，取件码，2 7 0 1 7",
            CodeSpeech.speechText(senderShort = "菜鸟驿站", codes = listOf("27017")),
        )
    }

    @Test
    fun `speech text drops an empty station name`() {
        assertEquals(
            "取件码，2 7 0 1 7",
            CodeSpeech.speechText(senderShort = "  ", codes = listOf("27017")),
        )
    }

    @Test
    fun `content description matches what is spoken`() {
        assertEquals(
            "放大出示取件码 3 杠 3 杠 0 6 0 0 6",
            CodeSpeech.codeContentDescription(listOf("3-3-06006"), prefix = "放大出示取件码"),
        )
    }
}
