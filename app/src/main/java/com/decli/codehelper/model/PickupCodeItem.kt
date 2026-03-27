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

    val matchedRuleDisplay: String
        get() = matchedRules.joinToString(separator = " · ")

    val codeCount: Int
        get() = codes.size
}

