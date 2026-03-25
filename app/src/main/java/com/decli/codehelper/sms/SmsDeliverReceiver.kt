package com.decli.codehelper.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        setResultCode(Telephony.Sms.Intents.RESULT_SMS_HANDLED)
    }
}
