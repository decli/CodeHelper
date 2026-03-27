package com.decli.codehelper.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import android.text.Html
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.util.PickupCodeExtractor
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class SmsRepository(
    private val contentResolver: ContentResolver,
    private val extractor: PickupCodeExtractor = PickupCodeExtractor(),
) {
    fun loadPickupCodes(
        filterWindow: CodeFilterWindow,
        promptKeywords: List<String>,
        advancedRules: List<String>,
        pickedUpKeys: Set<String>,
        includePickedUp: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<PickupCodeItem> {
        val sinceMillis = nowMillis - (filterWindow.hours * 60L * 60L * 1000L)
        val results = mutableListOf<PickupCodeItem>()

        results += loadSmsItems(
            sinceMillis = sinceMillis,
            promptKeywords = promptKeywords,
            advancedRules = advancedRules,
            pickedUpKeys = pickedUpKeys,
            includePickedUp = includePickedUp,
        )
        results += loadMmsItems(
            sinceMillis = sinceMillis,
            promptKeywords = promptKeywords,
            advancedRules = advancedRules,
            pickedUpKeys = pickedUpKeys,
            includePickedUp = includePickedUp,
        )

        return sortForDisplay(results)
    }

    private fun loadSmsItems(
        sinceMillis: Long,
        promptKeywords: List<String>,
        advancedRules: List<String>,
        pickedUpKeys: Set<String>,
        includePickedUp: Boolean,
    ): List<PickupCodeItem> {
        val results = mutableListOf<PickupCodeItem>()
        val projection = arrayOf(
            BaseColumns._ID,
            Telephony.TextBasedSmsColumns.ADDRESS,
            Telephony.TextBasedSmsColumns.BODY,
            Telephony.TextBasedSmsColumns.DATE,
        )

        contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.TextBasedSmsColumns.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.TextBasedSmsColumns.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.DATE)

            while (cursor.moveToNext()) {
                val smsId = cursor.getLong(idIndex)
                val sender = cursor.getString(addressIndex).orEmpty().ifBlank { "短信" }
                val body = cursor.getString(bodyIndex).orEmpty()
                val receivedAt = cursor.getLong(dateIndex)

                appendMatches(
                    results = results,
                    messageType = MessageType.Sms,
                    messageId = smsId,
                    messageUri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId).toString(),
                    sender = sender,
                    body = body,
                    receivedAtMillis = receivedAt,
                    promptKeywords = promptKeywords,
                    advancedRules = advancedRules,
                    pickedUpKeys = pickedUpKeys,
                    includePickedUp = includePickedUp,
                )
            }
        }

        return results
    }

    private fun loadMmsItems(
        sinceMillis: Long,
        promptKeywords: List<String>,
        advancedRules: List<String>,
        pickedUpKeys: Set<String>,
        includePickedUp: Boolean,
    ): List<PickupCodeItem> {
        val results = mutableListOf<PickupCodeItem>()
        val projection = arrayOf(
            BaseColumns._ID,
            Telephony.BaseMmsColumns.DATE,
            Telephony.BaseMmsColumns.SUBJECT,
        )

        // Inference from AOSP TelephonyProvider/Messaging code: MMS date values are stored in seconds.
        val sinceSeconds = sinceMillis / 1000L

        contentResolver.query(
            Telephony.Mms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.BaseMmsColumns.DATE} >= ?",
            arrayOf(sinceSeconds.toString()),
            "${Telephony.BaseMmsColumns.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns.DATE)
            val subjectIndex = cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns.SUBJECT)

            while (cursor.moveToNext()) {
                val mmsId = cursor.getLong(idIndex)
                val receivedAtMillis = cursor.getLong(dateIndex) * 1000L
                val subject = cursor.getString(subjectIndex).orEmpty()
                val body = loadMmsBody(mmsId, subject)
                if (body.isBlank()) continue

                val sender = loadMmsSender(mmsId).ifBlank { "彩信" }

                appendMatches(
                    results = results,
                    messageType = MessageType.Mms,
                    messageId = mmsId,
                    messageUri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId).toString(),
                    sender = sender,
                    body = body,
                    receivedAtMillis = receivedAtMillis,
                    promptKeywords = promptKeywords,
                    advancedRules = advancedRules,
                    pickedUpKeys = pickedUpKeys,
                    includePickedUp = includePickedUp,
                )
            }
        }

        return results
    }

    private fun appendMatches(
        results: MutableList<PickupCodeItem>,
        messageType: MessageType,
        messageId: Long,
        messageUri: String,
        sender: String,
        body: String,
        receivedAtMillis: Long,
        promptKeywords: List<String>,
        advancedRules: List<String>,
        pickedUpKeys: Set<String>,
        includePickedUp: Boolean,
    ) {
        val extractedCodes = extractor.extract(
            body = body,
            promptKeywords = promptKeywords,
            advancedRules = advancedRules,
        )
        if (extractedCodes.isEmpty()) {
            return
        }

        val uniqueKey = buildMessageKey(
            messageType = messageType,
            messageId = messageId,
        )
        val isPickedUp = uniqueKey in pickedUpKeys || extractedCodes.any { extractedCode ->
            buildUniqueKey(
                messageType = messageType,
                messageId = messageId,
                code = extractedCode.code,
            ) in pickedUpKeys
        }

        if (!isPickedUp || includePickedUp) {
            results += PickupCodeItem(
                uniqueKey = uniqueKey,
                smsId = messageId,
                messageUri = messageUri,
                codes = extractedCodes.map { it.code },
                sender = sender,
                body = body,
                preview = body.compactPreview(),
                receivedAtMillis = receivedAtMillis,
                matchedRules = extractedCodes.map { it.matchedRule }.distinct(),
                isPickedUp = isPickedUp,
            )
        }
    }

    private fun loadMmsBody(mmsId: Long, subject: String): String {
        val parts = mutableListOf<String>()

        contentResolver.query(
            MMS_PARTS_URI,
            arrayOf(BaseColumns._ID, "ct", "text", "_data", "chset"),
            "mid = ?",
            arrayOf(mmsId.toString()),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val contentTypeIndex = cursor.getColumnIndexOrThrow("ct")
            val textIndex = cursor.getColumnIndexOrThrow("text")
            val dataIndex = cursor.getColumnIndexOrThrow("_data")
            val charsetIndex = cursor.getColumnIndexOrThrow("chset")

            while (cursor.moveToNext()) {
                val contentType = cursor.getString(contentTypeIndex).orEmpty()
                if (isIgnoredPart(contentType)) continue

                val inlineText = cursor.getString(textIndex).orEmpty()
                val dataPointer = cursor.getString(dataIndex)
                val charsetValue = cursor.getString(charsetIndex)

                val partText = when {
                    inlineText.isNotBlank() -> inlineText.normalizePartText(contentType)
                    !dataPointer.isNullOrBlank() -> {
                        val partId = cursor.getLong(idIndex)
                        readMmsPartText(
                            partId = partId,
                            contentType = contentType,
                            charsetValue = charsetValue,
                        )
                    }
                    else -> ""
                }

                if (partText.isNotBlank()) {
                    parts += partText
                }
            }
        }

        if (subject.isNotBlank()) {
            parts.add(0, subject)
        }

        return parts.joinToString(separator = "\n").trim()
    }

    private fun readMmsPartText(
        partId: Long,
        contentType: String,
        charsetValue: String?,
    ): String =
        runCatching {
            contentResolver.openInputStream(ContentUris.withAppendedId(MMS_PARTS_URI, partId))
                ?.use { input ->
                    val bytes = input.readBytes()
                    decodePartBytes(bytes, charsetValue).normalizePartText(contentType)
                }
                .orEmpty()
        }.getOrDefault("")

    private fun loadMmsSender(mmsId: Long): String {
        val addressUri = Uri.parse("content://mms/$mmsId/addr")
        contentResolver.query(
            addressUri,
            arrayOf("address"),
            "type = ?",
            arrayOf(MMS_FROM_ADDRESS_TYPE.toString()),
            null,
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow("address")
            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIndex).orEmpty().trim()
                if (address.isNotBlank() && !address.equals("insert-address-token", ignoreCase = true)) {
                    return address
                }
            }
        }
        return ""
    }

    companion object {
        private val MMS_PARTS_URI: Uri = Uri.parse("content://mms/part")
        private const val MMS_FROM_ADDRESS_TYPE = 137
        private val mediaPrefixes = listOf("image/", "audio/", "video/")
        private val skippedContentTypes = setOf(
            "application/smil",
            "application/octet-stream",
        )

        fun buildMessageKey(messageType: MessageType, messageId: Long): String =
            when (messageType) {
                MessageType.Sms -> "sms:$messageId"
                MessageType.Mms -> "mms:$messageId"
            }

        fun buildUniqueKey(messageType: MessageType, messageId: Long, code: String): String =
            when (messageType) {
                MessageType.Sms -> "$messageId|${code.uppercase()}"
                MessageType.Mms -> "mms:$messageId|${code.uppercase()}"
            }

        fun sortForDisplay(items: List<PickupCodeItem>): List<PickupCodeItem> =
            items.sortedWith(
                compareBy<PickupCodeItem> { it.isPickedUp }
                    .thenByDescending { it.receivedAtMillis }
                    .thenByDescending { it.smsId },
            )
    }

    enum class MessageType {
        Sms,
        Mms,
    }

    private fun isIgnoredPart(contentType: String): Boolean {
        val normalized = contentType.lowercase()
        return normalized in skippedContentTypes || mediaPrefixes.any(normalized::startsWith)
    }

    private fun decodePartBytes(bytes: ByteArray, charsetValue: String?): String {
        if (bytes.isEmpty()) return ""
        val candidates = buildList {
            charsetValue?.toIntOrNull()?.let { mib ->
                charsetFromMib(mib)?.let(::add)
            }
            add(StandardCharsets.UTF_8)
            add(StandardCharsets.UTF_16)
            add(StandardCharsets.UTF_16LE)
            add(StandardCharsets.UTF_16BE)
            add(Charsets.ISO_8859_1)
        }.distinct()

        return candidates
            .asSequence()
            .map { charset -> runCatching { bytes.toString(charset) }.getOrNull().orEmpty() }
            .firstOrNull { it.isLikelyText() }
            ?: bytes.toString(StandardCharsets.UTF_8)
    }

    private fun charsetFromMib(mib: Int): Charset? =
        when (mib) {
            3, 2252 -> Charsets.ISO_8859_1
            106 -> StandardCharsets.UTF_8
            1015 -> StandardCharsets.UTF_16
            1013 -> StandardCharsets.UTF_16BE
            1014 -> StandardCharsets.UTF_16LE
            else -> null
        }
}

private fun String.compactPreview(maxLength: Int = 78): String {
    val normalized = replace(Regex("""\s+"""), " ").trim()
    return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 1) + "…"
}

private fun String.normalizePartText(contentType: String): String {
    val normalizedContentType = contentType.lowercase()
    val text = if (
        normalizedContentType.contains("html") ||
        normalizedContentType.contains("xml") ||
        normalizedContentType.contains("xhtml")
    ) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        this
    }

    return text
        .replace('\u0000', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun String.isLikelyText(): Boolean {
    val cleaned = replace('\u0000', ' ').trim()
    if (cleaned.isBlank()) return false
    val printableCount = cleaned.count { it.isWhitespace() || !it.isISOControl() }
    return printableCount >= (cleaned.length * 0.8)
}
