package com.decli.codehelper.model

data class PickupCodeItem(
    val uniqueKey: String,
    val smsId: Long,
    val messageUri: String? = null,
    val codes: List<String>,
    /** 发件号码，只用来打开短信会话；界面上显示的是 [station] */
    val sender: String,
    val body: String,
    val preview: String,
    val receivedAtMillis: Long,
    val matchedRules: List<String>,
    val isPickedUp: Boolean,
    /** 从签名 / 正文认出的驿站名，如「菜鸟驿站」；认不出时为空，见 [com.decli.codehelper.util.StationName] */
    val station: String = "",
) {
    val codeDisplay: String
        get() = codes.joinToString(separator = "\n")

    val codeCount: Int
        get() = codes.size

    /**
     * 卡头与分组标题用的驿站名，如「菜鸟驿站」「兔喜生活」。
     * 认不出驿站时不退回发件号码：1068… 开头的平台号又长又没意义，还会把同一家驿站拆成好几组。
     */
    val senderShort: String
        get() = if (station.isBlank()) UNKNOWN_STATION else shortenSender(station)

    companion object {
        const val UNKNOWN_STATION = "未注明驿站"
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
