package de.codehat.remotebatterystatus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import fi.iki.elonen.NanoHTTPD
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteOrder
import java.security.KeyStore
import java.util.*
import java.util.logging.Logger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext


class MainActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_PORT: Int = 8080

        private var isStarted: Boolean = false
    }

    private var webServer: RemoteBatteryStatusWebServer? = null
    private var broadcastReceiverNetworkState: BroadcastReceiver? = null

    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var editTextPort: EditText
    private lateinit var floatingActionButtonOnOff: FloatingActionButton
    private lateinit var textViewMessage: View
    private lateinit var textViewIpAddress: TextView

    private val randomNotificationId = Random().nextInt(1000)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // Init view
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        editTextPort = findViewById(R.id.editTextPort)
        floatingActionButtonOnOff = findViewById(R.id.floatingActionButtonOnOff)
        textViewMessage = findViewById(R.id.textViewMessage)
        textViewIpAddress = findViewById(R.id.textViewIpAddress)
        setIpAccess()
        floatingActionButtonOnOff = findViewById(R.id.floatingActionButtonOnOff)
        floatingActionButtonOnOff.setOnClickListener {
            if (isConnectedInWifi()) {
                if (!isStarted && startWebServer()) {
                    isStarted = true
                    textViewMessage.visibility = View.VISIBLE
                    floatingActionButtonOnOff.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorServerOn)
                    editTextPort.isEnabled = false
                } else if (stopWebServer()) {
                    isStarted = false
                    textViewMessage.visibility = View.INVISIBLE
                    floatingActionButtonOnOff.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorServerOff)
                    editTextPort.isEnabled = true
                }
            } else {
                Snackbar.make(coordinatorLayout, getString(R.string.wifi_message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "main")
                .setSmallIcon(R.drawable.ic_stat_server_status)
                .setContentTitle("Remote Battery Status")
                .setContentText("Server is running on ${getIpAccess()}:${getPortFromEditText()}!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)

        NotificationManagerCompat.from(this).apply {
            notify(randomNotificationId, mBuilder.build())
        }
    }

    private fun startWebServer(): Boolean {
        if (!isStarted) {
            val port = getPortFromEditText()
            try {
                if (port == 0) {
                    throw Exception()
                }
                webServer = RemoteBatteryStatusWebServer(applicationContext, textViewIpAddress.text.toString(), port)

                // Thanks to:
                // - https://stackoverflow.com/a/3027528
                // - https://stackoverflow.com/a/11118072
                // - https://stackoverflow.com/q/31270613
                // - https://github.com/NanoHttpd/nanohttpd/issues/139#issuecomment-108822146
                val ks = KeyStore.getInstance("BKS")
                ks.load(applicationContext.assets.open("keystore.jks"), "password".toCharArray())
                val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(ks, "password".toCharArray())
                val sslContext: SSLContext = SSLContext.getInstance("TLS")
                sslContext.init(kmf.keyManagers, null, null)

                webServer!!.makeSecure(sslContext.serverSocketFactory, null)
                webServer!!.start()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                Snackbar.make(coordinatorLayout, "The port $port doesn't work, please change it between 1000 and 9999.", Snackbar.LENGTH_LONG).show()
            }
        }
        return false
    }

    private fun stopWebServer(): Boolean {
        if (isStarted && webServer != null) {
            webServer!!.stop()
            return true
        }
        return false
    }

    private fun setIpAccess() {
        textViewIpAddress.text = getIpAccess()
    }

    private fun getIpAccess(): String {
        val wifiManager: WifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var ipAddress: Int = wifiManager.connectionInfo.ipAddress

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }

        val ipByteArray: ByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()

        return InetAddress.getByAddress(ipByteArray).hostAddress
    }

    private fun getPortFromEditText(): Int {
        val valueEditText = editTextPort.text.toString()
        return if (valueEditText.isNotEmpty()) valueEditText.toInt() else DEFAULT_PORT
    }

    fun isConnectedInWifi(): Boolean {
        val wifiManager: WifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val networkInfo: NetworkInfo? = (applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        if (networkInfo != null && networkInfo.isAvailable && networkInfo.isConnected
            && wifiManager.isWifiEnabled && networkInfo.typeName == "WIFI") {
            return true
        }
        return false
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("main", name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

}
