package de.codehat.remotebatterystatus.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import de.codehat.remotebatterystatus.MainActivity
import de.codehat.remotebatterystatus.R
import de.codehat.remotebatterystatus.RemoteBatteryStatusWebServer
import de.codehat.remotebatterystatus.receiver.StopServerReceiver
import de.codehat.remotebatterystatus.util.WifiUtil
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class WebServerService: Service() {

    private val serverHandler: Handler = Handler()

    private var webServer: RemoteBatteryStatusWebServer? = null

    private var hostname: String = "127.0.0.1"
    private var port: Int = 0
    private var isStarted: Boolean = false

    companion object {
        private const val notificationId = 42
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("WebServerService", "Received start id $startId : $intent")

        return if (!WifiUtil.isConnectedInWifi(applicationContext)) {
            stopSelf()
            START_NOT_STICKY
        } else {
            hostname = intent.getStringExtra("hostname") ?: WifiUtil.getIpAccess(applicationContext)
            port = intent.getIntExtra("port", port)
            showNotification()
            serverHandler.post {
                startWebServer()
            }
            START_STICKY
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("WebServerService", "Starting web server service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("WebServerService", "Stopping web server service.")
        //NotificationManagerCompat.from(applicationContext).cancel(randomNotificationId)
        stopWebServer()
        stopForeground(true)
    }

    private fun startWebServer(): Boolean {
        if (!isStarted) {
            Log.i("WebServerService", "Start web server...")
            isStarted = true
            try {
                if (port == 0) {
                    throw Exception("Port is missing.")
                }
                webServer = RemoteBatteryStatusWebServer(applicationContext, hostname, port)

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
                Toast.makeText(applicationContext, "The port $port doesn't work, please change it between 1000 and 9999.", Toast.LENGTH_LONG).show()
            }
        }
        return false
    }

    private fun stopWebServer(): Boolean {
        if (isStarted && webServer != null) {
            Log.i("WebServerService", "Stop web server...")
            isStarted = false
            webServer!!.stop()
            return true
        }
        return false
    }

    private fun showNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        val mainIntent = Intent(applicationContext, StopServerReceiver::class.java)
        val mainPendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, mainIntent, 0)

        val notification: Notification = NotificationCompat.Builder(this, "main")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_description, hostname, port))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .addAction(R.drawable.ic_stop_server, "STOP", mainPendingIntent)
                .build()

        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR

        startForeground(notificationId, notification)
    }
}