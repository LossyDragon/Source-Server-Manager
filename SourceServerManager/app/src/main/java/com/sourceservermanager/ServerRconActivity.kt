package com.sourceservermanager

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.sourceservermanager.rcon.RconConnection
import com.sourceservermanager.rcon.exception.*
import kotlinx.android.synthetic.main.activity_server_rcon.*

open class ServerRconActivity : AppCompatActivity() {

    companion object {
        private const val SSM_CMD = "::SSMCMD::"
        private const val TAG = "ServerRconActivity"

        const val EXTRA_ID = "com.sourceservermanager.EXTRA_ID"
        const val EXTRA_TITLE = "com.sourceservermanager.EXTRA_TITLE"
        const val EXTRA_IP = "com.sourceservermanager.EXTRA_IP"
        const val EXTRA_PORT = "com.sourceservermanager.EXTRA_PORT"
        const val EXTRA_PASSWORD = "com.sourceservermanager.EXTRA_PASSWORD"
    }

    //Server Variables
    private var nickname: String? = null
    private var address: String? = null
    private var port: Int? = null
    private var password: String? = null

    var serverResponse: String? = null

    private var connection: RconConnection? = null

    internal val mHandler = Handler()
    private val scrollHandler = Handler()

    // For Real-time logging
    private var chatModeActive = false
    private var logModeActive = false
    private var mTcpClient: TCPClient? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Create runnable for scrolling to bottom on scrollview
    private val scrollBottom: Runnable = Runnable {
        // Force scroll to scroll to the bottom
        rconResponseScroll.fullScroll(ScrollView.FOCUS_DOWN)
    }

    // Create runnable for posting server response from thread
    internal val mUpdateResults: Runnable = Runnable {
        if (serverResponse != null) {
            rconResponse.append(serverResponse)
            // Force scroll to scroll to the bottom
            scrollHandler.postDelayed(scrollBottom, 10)

            Log.i(TAG, serverResponse)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_rcon)

        if (intent.hasExtra(EXTRA_ID)) {
            nickname = intent.getStringExtra(EXTRA_TITLE)
            address = intent.getStringExtra(EXTRA_IP)
            port = intent.getStringExtra(EXTRA_PORT).toInt()
            password = intent.getStringExtra(EXTRA_PASSWORD)
        }

        Log.i(TAG, "$nickname/$address/$port/$password")

        title = if (nickname!!.isBlank())
            String.format(resources.getString(R.string.title_rcon_activity), address)
        else
            String.format(resources.getString(R.string.title_rcon_activity), nickname)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sendButton.setOnClickListener { buttonClicked(false) }
        sayButton.setOnClickListener { buttonClicked(true) }

        button_more.setOnClickListener {
            val popup = PopupMenu(this@ServerRconActivity, it)
            popup.inflate(R.menu.menu_rcon_more)

            popup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.action_users -> {
                        threadRconRequest(false, arrayOf("users"))
                    }
                    R.id.action_status -> {
                        threadRconRequest(false, arrayOf("status"))
                    }
                }
                true
            }

            popup.show()
        }
    }

    override fun onPause() {
        super.onPause()
        connection?.close()
        connection = null
    }

    override fun onDestroy() {
        super.onDestroy()

        nickname = null
        address = null
        port = null
        password = null

        // Disable chat mode before we exit
        // NOTE: This also calls releaseWakeLock()
        if (chatModeActive || logModeActive)
            disableLogMode()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_rcon, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_chat -> {
                if (chatModeActive) {
                    // Disable chat mode
                    disableLogMode()

                    chatModeActive = false
                } else {
                    if (!logModeActive) {
                        // Enable chat mode
                        enableLogMode()
                    } else {
                        logModeActive = false
                    }

                    chatModeActive = true
                }
                return true
            }
            R.id.action_clear_log -> {
                rconResponse.text = ""
                // Force scroll to scroll to the bottom
                scrollHandler.postDelayed(scrollBottom, 10)

                return true
            }
            R.id.action_log -> {
                if (logModeActive) {
                    // Disable chat mode
                    disableLogMode()

                    logModeActive = false
                } else {
                    if (!chatModeActive) {
                        // Enable chat mode
                        enableLogMode()
                    } else {
                        chatModeActive = false
                    }

                    logModeActive = true
                }

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun disableLogMode() {
        // Disable chat mode
        if (mTcpClient != null) {
            mTcpClient!!.stopClient()
            mTcpClient = null
        }

        releaseWakeLock()

        chatEnabledBanner.visibility = View.GONE

        // Send commands to server to run
        threadRconRequest(false, arrayOf("logaddress_del $address:$port", "log off"))
    }

    private fun enableLogMode() {
        // Real-time logging connection
        ConnectTask().execute("")

        // Get a wakelock so we don't disconnect from the socket
        releaseWakeLock()
        acquireWakeLock()

        chatEnabledBanner.visibility = View.VISIBLE

        // Send commands to server to run
        threadRconRequest(false, arrayOf("logaddress_add $address:$port", "log off", "log on"))
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sourceservermanager:SSM Logging Lock")
        wakeLock!!.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        // Release the wakelock, if it's held
        Log.d(TAG, "Releasing Wakelock")
        if (wakeLock != null) {
            if (wakeLock!!.isHeld) {
                wakeLock!!.release()
            }
        }
    }

    private fun buttonClicked(isSay: Boolean) {
        threadRconRequest(isSay, arrayOf(rconCommand.text.toString()))
        rconCommand.setText("")
    }

    fun sendRconRequest(command: String) {

        serverResponse = try {

            if(connection == null)
                connection = RconConnection(address!!, port!!, password!!)

            connection!!.send(command)

        } catch (e: NotOnlineException) {
            getString(R.string.error_not_online)
        } catch (e: AuthenticationException) {
            getString(R.string.error_bad_rcon)
        } catch (e: ConnectException) {
            getString(R.string.error_failed_rcon)
        } catch (e: ResponseEmptyException) {
            getString(R.string.error_empty_rcon)
        } catch (e: TimeoutException) {
            getString(R.string.error_socket_timeout)
        }
    }

    private fun threadRconRequest(isSay: Boolean, commands: Array<String>): Boolean {
        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        val t = object : Thread() {
            override fun run() {
                var isLogCommand = false
                if (isSay) {
                    for (command in commands) {
                        sendRconRequest("say $command")
                    }
                } else {
                    for (command in commands) {
                        if (command.length >= 3) {
                            if (command.substring(0, 3) === "log") {
                                isLogCommand = true
                            }
                            sendRconRequest(command)
                        }
                    }
                }

                // We won't send response to our textview since we're going to
                // see it with our log listener
                // This should resolve the duplicate message issue
                if (chatModeActive) {
                    if (!isSay && !isLogCommand) {
                        if (commands[0].toLowerCase() === "status") {
                            // Don't touch response
                        } else {
                            // Clean up any message while logging is on, as they are quite verbose
                            val lines = serverResponse!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            serverResponse = ""
                            for (line in lines) {
                                if (!line.startsWith("L")) {
                                    serverResponse = serverResponse + line + "\n"
                                }
                            }
                        }
                        mHandler.post(mUpdateResults)
                    }
                } else {
                    mHandler.post(mUpdateResults)
                }
            }
        }
        t.start()
        return true
    }

    //This is bad
    @SuppressLint("StaticFieldLeak")
    inner class ConnectTask : AsyncTask<String, String, TCPClient>() {
        override fun doInBackground(vararg message: String): TCPClient? {

            //we create a TCPClient object and
            mTcpClient = TCPClient(object : TCPClient.OnMessageReceived {
                override fun messageReceived(message: String) {
                    //here the messageReceived method is implemented
                    //this method calls the onProgressUpdate
                    publishProgress(message)
                }
            })
            // Sends the message to the server with the host we want to get logs for
            mTcpClient!!.run("$address:$port", address!!, port!!)

            return null
        }

        override fun onProgressUpdate(vararg values: String) {
            super.onProgressUpdate(*values)

            var tempResp = values[0]

            // Intercept any SSM messages
            val ssmCmdIndex = tempResp.indexOf(SSM_CMD)
            if (ssmCmdIndex >= 0) {
                // It's a SSMCMD, check what the 2-character command is
                val startIndex = ssmCmdIndex + SSM_CMD.length
                val ssmCmd = tempResp.substring(startIndex, startIndex + 2)

                //val check = ssmCmd.equals("UD", ignoreCase = true)
                if (ssmCmd.equals("UD", ignoreCase = true)) {
                    serverResponse = getString(R.string.warning_remote_disconnect)
                    mHandler.post(mUpdateResults)

                    disableLogMode()
                }
            } else {

                // Filter server log for say and say_team messages
                tempResp = tempResp.substring(tempResp.indexOf(":") + 8)

                val filterList = arrayOf("\" say \"", "\" say_team \"")

                if (chatModeActive) {
                    for (filter in filterList) {
                        val sayIndex = tempResp.indexOf(filter)
                        if (sayIndex > 0) {
                            val userName = tempResp.substring(1, tempResp.indexOf("<"))
                            val msg = tempResp.substring(tempResp.indexOf("\"", sayIndex + 1) + 1, tempResp.lastIndexOf("\""))

                            serverResponse = if (filter.contains("say_team")) {
                                "$userName<T>: $msg\n"
                            } else {
                                "$userName: $msg\n"
                            }

                            mHandler.post(mUpdateResults)
                        }
                    }
                } else {
                    serverResponse = tempResp
                    mHandler.post(mUpdateResults)
                }
            }
        }
    }
}
