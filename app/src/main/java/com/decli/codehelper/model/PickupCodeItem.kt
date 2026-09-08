package com.decli.codehelper.model

data class PickupCodeItem(
    val uniqueKey: String,
    val smsId: Long,
    val messageUri: String? = null,
    val codes: List<String>,
    val sender: String,
    val body: String,
    val preview: String,
    val receivedAtMillis: Long,
    val matchedRules: List<String>,
    val isPickedUp: Boolean,
) {
    val codeDisplay: String
        get() = codes.joinToString(separator = "\n")

    val codeCount: Int
        get() = codes.size

    /** 卡头与分组标题用的短发件方，如「菜鸟驿站」「兔喜快递」 */
    val senderShort: String
        get() = shortenSender(sender)

    companion object {
        private const val MAX_SENDER_SHORT_LENGTH = 6

        /**
         * 去掉【】方括号后取首段（按「 · 」/ 空格 / 逗号切分），超长截断到 6 字。
         * 纯号码类发件方保持完整：号码截断后会变成另一个号码，比过长更糟。
         */
        fun shortenSender(sender: String): String {
            val cleaned = sender
                .replace(Regex("[【】\\[\\]]"), " ")
                .trim()
            val head = cleaned
                .split(" · ", "·", " ", "　", "，", ",")
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
            val base = head.ifEmpty { cleaned }.ifEmpty { "短信" }
            val isNumberLike = base.all { it.isDigit() || it == '+' || it == '-' }
            return if (isNumberLike || base.length <= MAX_SENDER_SHORT_LENGTH) {
                base
            } else {
                base.take(MAX_SENDER_SHORT_LENGTH)
            }
        }
    }
}
