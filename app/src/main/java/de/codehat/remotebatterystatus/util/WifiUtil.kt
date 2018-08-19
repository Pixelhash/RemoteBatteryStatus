package de.codehat.remotebatterystatus.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteOrder

class WifiUtil {
    companion object {
        fun getIpAccess(context: Context): String {
            val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var ipAddress: Int = wifiManager.connectionInfo.ipAddress

            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                ipAddress = Integer.reverseBytes(ipAddress)
            }

            val ipByteArray: ByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()

            return InetAddress.getByAddress(ipByteArray).hostAddress
        }

        fun isConnectedInWifi(context: Context): Boolean {
            val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val networkInfo: NetworkInfo? = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
            if (networkInfo != null && networkInfo.isAvailable && networkInfo.isConnected
                    && wifiManager.isWifiEnabled && networkInfo.typeName == "WIFI") {
                return true
            }
            return false
        }
    }
}