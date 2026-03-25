package com.decli.codehelper.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.util.PickupCodeExtractor

class SmsRepository(
    private val contentResolver: ContentResolver,
    private val extractor: PickupCodeExtractor = PickupCodeExtractor(),
) {
    fun loadPickupCodes(
        filterWindow: CodeFilterWindow,
        rules: List<String>,
        pickedUpKeys: Set<String>,
        includePickedUp: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<PickupCodeItem> {
        val sinceMillis = nowMillis - (filterWindow.hours * 60L * 60L * 1000L)
        val results = mutableListOf<PickupCodeItem>()

        results += loadSmsItems(
            sinceMillis = sinceMillis,
            rules = rules,
            pickedUpKeys = pickedUpKeys,
            includePickedUp = includePickedUp,
        )
        results += loadMmsItems(
            sinceMillis = sinceMillis,
            rules = rules,
            pickedUpKeys = pickedUpKeys,
            includePickedUp = includePickedUp,
        )

        return sortForDisplay(results)
    }

    private fun loadSmsItems(
        sinceMillis: Long,
        rules: List<String>,
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
            Telephony.Sms.CONTENT_URI,
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
                    rules = rules,
                    pickedUpKeys = pickedUpKeys,
                    includePickedUp = includePickedUp,
                )
            }
        }

        return results
    }

    private fun loadMmsItems(
        sinceMillis: Long,
        rules: List<String>,
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
            Telephony.Mms.CONTENT_URI,
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
                    rules = rules,
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
        rules: List<String>,
        pickedUpKeys: Set<String>,
        includePickedUp: Boolean,
    ) {
        extractor.extract(body = body, rules = rules).forEach { extractedCode ->
            val uniqueKey = buildUniqueKey(
                messageType = messageType,
                messageId = messageId,
                code = extractedCode.code,
            )
            val isPickedUp = uniqueKey in pickedUpKeys
            if (!isPickedUp || includePickedUp) {
                results += PickupCodeItem(
                    uniqueKey = uniqueKey,
                    smsId = messageId,
                    messageUri = messageUri,
                    code = extractedCode.code,
                    sender = sender,
                    body = body,
                    preview = body.compactPreview(),
                    receivedAtMillis = receivedAtMillis,
                    matchedRule = extractedCode.matchedRule,
                    isPickedUp = isPickedUp,
                )
            }
        }
    }

    private fun loadMmsBody(mmsId: Long, subject: String): String {
        val parts = mutableListOf<String>()

        contentResolver.query(
            MMS_PARTS_URI,
            arrayOf(BaseColumns._ID, "ct", "text", "_data"),
            "mid = ?",
            arrayOf(mmsId.toString()),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val contentTypeIndex = cursor.getColumnIndexOrThrow("ct")
            val textIndex = cursor.getColumnIndexOrThrow("text")
            val dataIndex = cursor.getColumnIndexOrThrow("_data")

            while (cursor.moveToNext()) {
                val contentType = cursor.getString(contentTypeIndex).orEmpty()
                if (!contentType.startsWith("text/", ignoreCase = true)) continue

                val inlineText = cursor.getString(textIndex).orEmpty()
                val dataPointer = cursor.getString(dataIndex)

                val partText = when {
                    inlineText.isNotBlank() -> inlineText
                    !dataPointer.isNullOrBlank() -> {
                        val partId = cursor.getLong(idIndex)
                        readMmsPartText(partId)
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

    private fun readMmsPartText(partId: Long): String =
        runCatching {
            contentResolver.openInputStream(ContentUris.withAppendedId(MMS_PARTS_URI, partId))
                ?.bufferedReader()
                ?.use { it.readText() }
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
}

private fun String.compactPreview(maxLength: Int = 78): String {
    val normalized = replace(Regex("""\s+"""), " ").trim()
    return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 1) + "…"
}
