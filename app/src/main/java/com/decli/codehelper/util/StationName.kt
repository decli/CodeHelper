package com.decli.codehelper.util

/**
 * 从短信里认出「哪个驿站发来的」（纯函数，不依赖 Android，便于单元测试）。
 *
 * 发件号码多是 1068… 开头的短信平台号，同一个驿站会换着号码发，号码本身对老人没有意义。
 * 按可信度依次尝试：
 * 1. 短信签名：正文开头或结尾的【菜鸟驿站】——国内营销 / 通知短信按规定都带签名；
 * 2. 发件方本身就是名字（部分系统把 5G 消息 / 企业号的名称直接存进 address）；
 * 3. 同一号码发过的其他短信里的签名——个别短信（如 5G 消息）正文不带签名，
 *    但同一个号码之前的短信往往带着；
 * 4. 正文里出现的常见驿站 / 快递品牌名，如「到兔喜生活 30 号楼店取件」。
 * 都认不出时返回空串，由界面显示统一的兜底文案，而不是一长串号码。
 */
object StationName {

    private const val MAX_SIGNATURE_LENGTH = 20

    private val leadingSignature = Regex("""^\s*[【\[［]([^】\]］]{1,$MAX_SIGNATURE_LENGTH})[】\]］]""")
    private val trailingSignature = Regex("""[【\[［]([^】\]］]{1,$MAX_SIGNATURE_LENGTH})[】\]］][\s。.!！]*$""")

    /** 签名里「菜鸟驿站（龙腾苑店）」「菜鸟驿站(xx)」只取品牌部分 */
    private val parenthetical = Regex("""[（(].*$""")

    /**
     * 正文里可识别的品牌名 → 显示名。按出现位置取最靠前的一个；
     * 同一位置上长名字优先（「菜鸟驿站」先于「菜鸟」）。
     */
    private val knownBrands: List<Pair<String, String>> = listOf(
        "菜鸟驿站" to "菜鸟驿站",
        "菜鸟" to "菜鸟驿站",
        "兔喜生活" to "兔喜生活",
        "兔喜" to "兔喜生活",
        "妈妈驿站" to "妈妈驿站",
        "丰巢" to "丰巢",
        "递管家" to "递管家",
        "近邻宝" to "近邻宝",
        "熊猫快收" to "熊猫快收",
        "快递超市" to "快递超市",
        "中邮驿站" to "中邮驿站",
        "邮政" to "中国邮政",
        "京东" to "京东快递",
        "顺丰" to "顺丰速运",
        "中通" to "中通快递",
        "圆通" to "圆通速递",
        "韵达" to "韵达快递",
        "申通" to "申通快递",
        "极兔" to "极兔速递",
        "德邦" to "德邦快递",
    ).sortedByDescending { it.first.length }

    /**
     * @param senderHistorySignature 同一号码其他短信里的签名（调用方负责查询与缓存），
     * 只在本条短信自己认不出时才会被调用。
     */
    fun resolve(
        body: String,
        address: String,
        senderHistorySignature: () -> String? = { null },
    ): String =
        fromSignature(body)
            ?: fromAddress(address)
            ?: senderHistorySignature()
            ?: fromBrandKeyword(body)
            ?: ""

    /** 正文开头或结尾的【签名】；中间的【】（如「凭【6-4-12008】」）不算 */
    fun fromSignature(body: String): String? {
        val raw = leadingSignature.find(body)?.groupValues?.get(1)
            ?: trailingSignature.find(body)?.groupValues?.get(1)
            ?: return null
        return cleanName(raw)
    }

    /** 发件方已经是名字（含汉字或字母、不是号码 / sip 地址）时直接用 */
    fun fromAddress(address: String): String? {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains('@') || trimmed.contains(':')) return null
        return cleanName(trimmed)
    }

    fun fromBrandKeyword(body: String): String? =
        knownBrands
            .mapNotNull { (keyword, display) ->
                val index = body.indexOf(keyword)
                if (index >= 0) index to display else null
            }
            .minByOrNull { it.first }
            ?.second

    private fun cleanName(raw: String): String? {
        val name = raw
            .replace(parenthetical, "")
            .trim()
        // 必须含文字：纯数字 / 符号（号码、取件码）不是驿站名
        return name.takeIf { it.isNotEmpty() && it.any(Char::isLetter) }
    }
}
