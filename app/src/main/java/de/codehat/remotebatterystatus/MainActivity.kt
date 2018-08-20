package de.codehat.remotebatterystatus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import de.codehat.remotebatterystatus.service.WebServerService
import de.codehat.remotebatterystatus.util.ServiceUtil
import de.codehat.remotebatterystatus.util.WifiUtil

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_PORT: Int = 8080
    }

    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var editTextPort: EditText
    private lateinit var floatingActionBtnOnOff: FloatingActionButton
    private lateinit var textViewInfoMessage: View
    private lateinit var textViewIpAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        createNotificationChannel()

        // Init view
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        editTextPort = findViewById(R.id.editTextPort)
        floatingActionBtnOnOff = findViewById(R.id.floatingActionBtnOnOff)
        textViewInfoMessage = findViewById(R.id.textViewInfoMessage)
        textViewIpAddress = findViewById(R.id.textViewIpAddress)
        setIpAccess()
        floatingActionBtnOnOff = findViewById(R.id.floatingActionBtnOnOff)

        floatingActionBtnOnOff.setOnClickListener {
            if (WifiUtil.isConnectedInWifi(applicationContext)) {
                if (!ServiceUtil.isMyServiceRunning(applicationContext, WebServerService::class.java) && startWebServer()) {
                    updateUi()
                } else if (stopWebServer()) {
                    updateUi()
                }
            } else {
                Snackbar.make(coordinatorLayout, getString(R.string.wifi_message), Snackbar.LENGTH_LONG).show()
            }
        }

        // Update UI right away.
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startWebServer(): Boolean {
        if (!ServiceUtil.isMyServiceRunning(applicationContext, WebServerService::class.java)) {
            val port = getPortFromEditText()
            val hostname = WifiUtil.getIpAccess(applicationContext)
            val service = Intent(applicationContext, WebServerService::class.java)
            with(service) {
                putExtra("hostname", hostname)
                putExtra("port", port)
            }
            startService(service)
            return true
        }
        return false
    }

    private fun stopWebServer(): Boolean {
        if (ServiceUtil.isMyServiceRunning(applicationContext, WebServerService::class.java)) {
            val service = Intent(applicationContext, WebServerService::class.java)
            stopService(service)
            return true
        }
        return false
    }

    private fun setIpAccess() {
        textViewIpAddress.text = WifiUtil.getIpAccess(applicationContext)
    }

    private fun getPortFromEditText(): Int {
        val valueEditText = editTextPort.text.toString()
        return if (valueEditText.isNotEmpty()) valueEditText.toInt() else DEFAULT_PORT
    }

    private fun updateUi() {
        if (ServiceUtil.isMyServiceRunning(applicationContext, WebServerService::class.java)) {
            textViewInfoMessage.visibility = View.VISIBLE
            floatingActionBtnOnOff.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorServerOn)
            editTextPort.isEnabled = false
        } else {
            textViewInfoMessage.visibility = View.INVISIBLE
            floatingActionBtnOnOff.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorServerOff)
            editTextPort.isEnabled = true
        }
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
