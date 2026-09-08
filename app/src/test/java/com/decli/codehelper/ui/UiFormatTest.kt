package com.decli.codehelper.ui

import com.decli.codehelper.model.CodeFilterWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormatTest {

    @Test
    fun `range labels stay spoken-language`() {
        assertEquals("最近 12 小时", rangeLabel(CodeFilterWindow.Last12Hours))
        assertEquals("最近 1 天", rangeLabel(CodeFilterWindow.Last1Day))
        assertEquals("最近 14 天", rangeLabel(CodeFilterWindow.Last14Days))
    }
}
