package de.codehat.remotebatterystatus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import android.os.BatteryManager

class RemoteBatteryStatusWebServer(private val context: Context,
                                   hostname: String = "127.0.0.1",
                                   port: Int): NanoHTTPD(hostname, port) {

    private var batteryPercentage: Int = getBatteryLevel().toInt()
    private var batteryChangedReceiver: BroadcastReceiver

    init {
        batteryChangedReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                batteryPercentage = getBatteryLevel(intent).toInt()

                println("Level has changed to $batteryPercentage!")
            }
        }
        context.registerReceiver(batteryChangedReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun serve(session: IHTTPSession): Response {
        return if (session.parameters.containsKey("valueOnly")) {
            newFixedLengthResponse((batteryPercentage).toString())
        } else {
            val allText = context.assets.open("index.html").bufferedReader().use(BufferedReader::readText)

            newFixedLengthResponse(allText)
        }
    }

    private fun getBatteryLevel(batteryIntent: Intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!): Float {
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return level.toFloat() / scale.toFloat() * 100.0f
    }

    override fun stop() {
        super.stop()

        // Unregister receiver.
        context.unregisterReceiver(batteryChangedReceiver)
    }
}