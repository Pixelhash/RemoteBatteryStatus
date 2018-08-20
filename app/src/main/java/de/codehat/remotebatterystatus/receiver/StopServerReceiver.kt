package de.codehat.remotebatterystatus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.codehat.remotebatterystatus.service.WebServerService
import de.codehat.remotebatterystatus.util.ServiceUtil

class StopServerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ServiceUtil.isMyServiceRunning(context, WebServerService::class.java)) {
            context.stopService(Intent(context, WebServerService::class.java))
        }
    }
}
