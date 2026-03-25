package com.decli.codehelper.model

data class PickupCodeItem(
    val uniqueKey: String,
    val smsId: Long,
    val code: String,
    val sender: String,
    val body: String,
    val preview: String,
    val receivedAtMillis: Long,
    val matchedRule: String,
    val isPickedUp: Boolean,
)

