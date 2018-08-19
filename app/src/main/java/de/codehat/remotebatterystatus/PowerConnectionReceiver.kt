package de.codehat.remotebatterystatus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import de.codehat.remotebatterystatus.service.WebServerService
import de.codehat.remotebatterystatus.util.ServiceUtil
import de.codehat.remotebatterystatus.util.WifiUtil

class PowerConnectionReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("PowerConnectionReceiver", "Called cause of battery! ${intent.action}")
        if (intent.action == Intent.ACTION_POWER_CONNECTED || intent.action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.i("PowerConnectionReceiver", "Con-/Disconnected to/from power source: ${intent.action}")

            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL

            if (WifiUtil.isConnectedInWifi(context)) {
                if (isCharging && !ServiceUtil.isMyServiceRunning(context, WebServerService::class.java)) {
                    Log.i("PowerConnectionReceiver", "Charging! Starting web server...")
                    val service = Intent(context, WebServerService::class.java)
                    service.putExtra("hostname", WifiUtil.getIpAccess(context))
                    context.startService(service)
                } else if (!isCharging && ServiceUtil.isMyServiceRunning(context, WebServerService::class.java)) {
                    Log.i("PowerConnectionReceiver", "Not charging! Stopping web server if running...")
                    val service = Intent(context, WebServerService::class.java)
                    context.stopService(service)
                }
            }
        }
    }
}