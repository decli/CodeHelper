package com.decli.codehelper.data

import android.content.ContentResolver
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
        deletedKeys: Set<String>,
        includeDeleted: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<PickupCodeItem> {
        val sinceMillis = nowMillis - (filterWindow.hours * 60L * 60L * 1000L)
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

                extractor.extract(body = body, rules = rules).forEach { extractedCode ->
                    val uniqueKey = buildUniqueKey(smsId = smsId, code = extractedCode.code)
                    val isDeleted = uniqueKey in deletedKeys
                    if (!isDeleted || includeDeleted) {
                        results += PickupCodeItem(
                            uniqueKey = uniqueKey,
                            smsId = smsId,
                            code = extractedCode.code,
                            sender = sender,
                            body = body,
                            preview = body.compactPreview(),
                            receivedAtMillis = receivedAt,
                            matchedRule = extractedCode.matchedRule,
                            isDeleted = isDeleted,
                        )
                    }
                }
            }
        }

        return results.sortedByDescending { it.receivedAtMillis }
    }

    companion object {
        fun buildUniqueKey(smsId: Long, code: String): String = "$smsId|${code.uppercase()}"
    }
}

private fun String.compactPreview(maxLength: Int = 78): String {
    val normalized = replace(Regex("""\s+"""), " ").trim()
    return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 1) + "…"
}

