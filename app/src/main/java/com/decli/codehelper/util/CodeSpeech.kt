package com.decli.codehelper.util

/**
 * 取件码朗读文本（纯函数，不依赖 Android，便于单元测试）。
 *
 * 规则：逐字朗读，连字符读「杠」，字母按字母读；多个码之间说「下一个」。
 * TalkBack 的 contentDescription 与朗读文本保持一致，避免读屏与语音播报口径不同。
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

    /** 语音播报全文：「菜鸟驿站，取件码，3 杠 3 杠 0 6 0 0 6」 */
    fun speechText(senderShort: String, codes: List<String>): String {
        val spoken = spellCodes(codes)
        return if (senderShort.isBlank()) "取件码，$spoken" else "$senderShort，取件码，$spoken"
    }

    /** 读屏描述：「放大出示取件码 3 杠 3 杠 0 6 0 0 6」 */
    fun codeContentDescription(codes: List<String>, prefix: String = "取件码"): String =
        "$prefix ${spellCodes(codes)}"
}
