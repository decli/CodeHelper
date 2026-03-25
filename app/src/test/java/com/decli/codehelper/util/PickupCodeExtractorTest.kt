package com.decli.codehelper.util

import com.decli.codehelper.data.SmsRepository
import com.decli.codehelper.model.PickupCodeItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PickupCodeExtractorTest {

    private val extractor = PickupCodeExtractor()

    @Test
    fun `extracts numeric code from default rule`() {
        val result = extractor.extract(
            body = "【快递】您的包裹已到站，取件码 62667148，请及时领取。",
            rules = PickupCodeExtractor.defaultRules,
        )

        assertEquals(listOf("62667148"), result.map { it.code })
    }

    @Test
    fun `extracts mixed code from custom rule without capture group`() {
        val result = extractor.extract(
            body = "您好，凭Y0986至菜鸟驿站取件。",
            rules = listOf("""凭[a-zA-Z0-9-]+"""),
        )

        assertEquals(listOf("Y0986"), result.map { it.code })
    }

    @Test
    fun `extracts hyphenated code only once even if multiple rules match`() {
        val result = extractor.extract(
            body = "取件码17-3-18014，凭17-3-18014领取。",
            rules = listOf(
                """取件码[：:\s]*([A-Za-z0-9-]+)""",
                """凭[：:\s]*([A-Za-z0-9-]+)""",
            ),
        )

        assertEquals(listOf("17-3-18014"), result.map { it.code })
    }

    @Test
    fun `sorts pending items ahead of picked up items`() {
        val items = listOf(
            pickupCodeItem(code = "Y0986", receivedAt = 1000L, isPickedUp = true),
            pickupCodeItem(code = "62667148", receivedAt = 3000L, isPickedUp = false),
            pickupCodeItem(code = "17-3-18014", receivedAt = 2000L, isPickedUp = false),
        )

        val result = SmsRepository.sortForDisplay(items)

        assertEquals(
            listOf("62667148", "17-3-18014", "Y0986"),
            result.map { it.code },
        )
    }

    private fun pickupCodeItem(
        code: String,
        receivedAt: Long,
        isPickedUp: Boolean,
    ) = PickupCodeItem(
        uniqueKey = "key-$code",
        smsId = receivedAt,
        code = code,
        sender = "短信",
        body = "测试短信 $code",
        preview = "测试短信 $code",
        receivedAtMillis = receivedAt,
        matchedRule = "测试规则",
        isPickedUp = isPickedUp,
    )
}
