package com.decli.codehelper.util

import com.decli.codehelper.data.SmsRepository
import com.decli.codehelper.model.PickupCodeItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PickupCodeExtractorTest {

    private val extractor = PickupCodeExtractor()

    @Test
    fun `extracts numeric code from default prompt keywords`() {
        val result = extractor.extract(
            body = "【快递】您的包裹已到站，取件码 62667148，请及时领取。",
            promptKeywords = PickupCodeExtractor.defaultPromptKeywords,
            advancedRules = PickupCodeExtractor.defaultAdvancedRules,
        )

        assertEquals(listOf("62667148"), result.map { it.code })
        assertEquals(listOf("命中规则1"), result.map { it.matchedRule })
    }

    @Test
    fun `extracts mixed code from custom advanced rule without capture group`() {
        val result = extractor.extract(
            body = "您好，凭Y0986至菜鸟驿站取件。",
            promptKeywords = emptyList(),
            advancedRules = listOf("""凭[a-zA-Z0-9-]+"""),
        )

        assertEquals(listOf("Y0986"), result.map { it.code })
    }

    @Test
    fun `extracts hyphenated code only once even if multiple prompt keywords match`() {
        val result = extractor.extract(
            body = "取件码17-3-18014，凭17-3-18014领取。",
            promptKeywords = listOf("取件码", "凭"),
            advancedRules = emptyList(),
        )

        assertEquals(listOf("17-3-18014"), result.map { it.code })
    }

    @Test
    fun `returns numbered rule label based on prompt keyword order`() {
        val result = extractor.extract(
            body = "您好，凭Y0986至菜鸟驿站取件。",
            promptKeywords = listOf("取件码", "凭"),
            advancedRules = emptyList(),
        )

        assertEquals(listOf("命中规则2"), result.map { it.matchedRule })
    }

    @Test
    fun `extracts code wrapped by Chinese quotes after 凭`() {
        val result = extractor.extract(
            body = """【菜鸟驿站】请23:00前到站凭“1-3-27017”到龙腾苑四区免喜生活30号楼店站点领取您的中通*15733包裹。""",
            promptKeywords = PickupCodeExtractor.defaultPromptKeywords,
            advancedRules = PickupCodeExtractor.defaultAdvancedRules,
        )

        assertEquals(listOf("1-3-27017"), result.map { it.code })
        assertEquals(listOf("命中规则5"), result.map { it.matchedRule })
    }

    @Test
    fun `extracts multiple codes after keyword with 为 and comma separators`() {
        val result = extractor.extract(
            body = "【菜鸟驿站】您有2个包裹在龙腾苑四区兔喜生活30号楼店。取件货码为3-3-06006，8-5-06003。",
            promptKeywords = PickupCodeExtractor.defaultPromptKeywords,
            advancedRules = PickupCodeExtractor.defaultAdvancedRules,
        )

        assertEquals(listOf("3-3-06006", "8-5-06003"), result.map { it.code })
        assertEquals(listOf("命中规则3", "命中规则3"), result.map { it.matchedRule })
    }

    @Test
    fun `extracts multiple codes after keyword with ideographic separators`() {
        val result = extractor.extract(
            body = "【菜鸟驿站】货码现在有 3-0 Y 包裹，请凭14-2-1016、15-4-20033、15-3-21029、16-4-20065尽早取。",
            promptKeywords = PickupCodeExtractor.defaultPromptKeywords,
            advancedRules = PickupCodeExtractor.defaultAdvancedRules,
        )

        assertEquals(
            listOf("14-2-1016", "15-4-20033", "15-3-21029", "16-4-20065"),
            result.map { it.code },
        )
        assertEquals(
            listOf("命中规则5", "命中规则5", "命中规则5", "命中规则5"),
            result.map { it.matchedRule },
        )
    }

    @Test
    fun `extracts code from expanded cargo keyword`() {
        val result = extractor.extract(
            body = "【快递】驿站码“Y0986”，请及时领取。",
            promptKeywords = PickupCodeExtractor.defaultPromptKeywords,
            advancedRules = PickupCodeExtractor.defaultAdvancedRules,
        )

        assertEquals(listOf("Y0986"), result.map { it.code })
        assertEquals(listOf("命中规则4"), result.map { it.matchedRule })
    }

    @Test
    fun `reports syntax error for invalid draft rule`() {
        assertEquals("正则语法错误", extractor.validationError("""取件码([A-Z"""))
    }

    @Test
    fun `uses different message keys for sms and mms items`() {
        assertEquals(
            "sms:7",
            SmsRepository.buildMessageKey(
                messageType = SmsRepository.MessageType.Sms,
                messageId = 7L,
            ),
        )
        assertEquals(
            "mms:7",
            SmsRepository.buildMessageKey(
                messageType = SmsRepository.MessageType.Mms,
                messageId = 7L,
            ),
        )
    }

    @Test
    fun `keeps legacy unique keys for code based picked state migration`() {
        assertEquals(
            "7|Y0986",
            SmsRepository.buildUniqueKey(
                messageType = SmsRepository.MessageType.Sms,
                messageId = 7L,
                code = "Y0986",
            ),
        )
        assertEquals(
            "mms:7|Y0986",
            SmsRepository.buildUniqueKey(
                messageType = SmsRepository.MessageType.Mms,
                messageId = 7L,
                code = "Y0986",
            ),
        )
    }

    @Test
    fun `sorts pending items ahead of picked up items`() {
        val items = listOf(
            pickupCodeItem(codes = listOf("Y0986"), receivedAt = 1000L, isPickedUp = true),
            pickupCodeItem(codes = listOf("62667148", "17-3-18014"), receivedAt = 3000L, isPickedUp = false),
            pickupCodeItem(codes = listOf("17-3-18014"), receivedAt = 2000L, isPickedUp = false),
        )

        val result = SmsRepository.sortForDisplay(items)

        assertEquals(
            listOf("62667148\n17-3-18014", "17-3-18014", "Y0986"),
            result.map { it.codeDisplay },
        )
    }

    private fun pickupCodeItem(
        codes: List<String>,
        receivedAt: Long,
        isPickedUp: Boolean,
    ) = PickupCodeItem(
        uniqueKey = "key-${codes.joinToString("-")}",
        smsId = receivedAt,
        messageUri = null,
        codes = codes,
        sender = "短信",
        body = "测试短信 ${codes.joinToString("、")}",
        preview = "测试短信 ${codes.joinToString("、")}",
        receivedAtMillis = receivedAt,
        matchedRules = listOf("命中规则1"),
        isPickedUp = isPickedUp,
    )
}
