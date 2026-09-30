package com.decli.codehelper.util

/**
 * 取件码的读屏文本（纯函数，不依赖 Android，便于单元测试）。
 *
 * 规则：逐字读，连字符读「杠」，字母按字母读；多个码之间说「下一个」。
 * 不这样拼的话，TalkBack 会把「27017」当整数念成「两万七千零一十七」。
 */
object CodeSpeech {

    /** 「3 杠 3 杠 0 6 0 0 6」 */
    fun spellCode(code: String): String =
        code
            .map { char -> if (char == '-') "杠" else char.toString() }
            .joinToString(separator = " ")

    /** 「3 杠 3 杠 0 6 0 0 6，下一个，8 杠 5 杠 0 6 0 0 3」 */
    fun spellCodes(codes: List<String>): String =
        codes.joinToString(separator = "，下一个，") { spellCode(it) }

    /** 读屏描述：「放大出示取件码 3 杠 3 杠 0 6 0 0 6」 */
    fun codeContentDescription(codes: List<String>, prefix: String = "取件码"): String =
        "$prefix ${spellCodes(codes)}"
}
