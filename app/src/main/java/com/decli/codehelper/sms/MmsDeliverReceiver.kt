package com.decli.codehelper.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        setResultCode(Activity.RESULT_OK)
    }
}
