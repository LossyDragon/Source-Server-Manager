package com.sourceservermanager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sourceservermanager.rcon.Rcon
import com.sourceservermanager.rcon.SourceRcon
import com.sourceservermanager.rcon.exception.BadRcon
import com.sourceservermanager.rcon.exception.ResponseEmpty
import kotlinx.android.synthetic.main.activity_server_rcon.*
import java.io.IOException
import java.net.SocketTimeoutException

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
    var hostname: String? = null
    var port: Int = 0
    private var password: String? = null

    var serverResponse: String? = null

    internal val mHandler = Handler()
    private val scrollHandler = Handler()

    // For Real-time logging
    private var chatModeActive = false
    private var logModeActive = false
    private var mTcpClient: TCPClient? = null
    private var mChatBanner: TextView? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Create runnable for scrolling to bottom on scrollview
    private val scrollBottom: Runnable = Runnable {
        val rconRepsonseScroll = findViewById<View>(R.id.rconResponseScroll) as ScrollView
        // Force scroll to scroll to the bottom
        rconRepsonseScroll.fullScroll(ScrollView.FOCUS_DOWN)
    }

    // Create runnable for posting server response from thread
    internal val mUpdateResults: Runnable = Runnable {
        if (serverResponse != null) {
            val rconRepsonseText = findViewById<View>(R.id.rconResponse) as TextView

            rconRepsonseText.append(serverResponse)
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
            hostname = intent.getStringExtra(EXTRA_IP)
            port = intent.getStringExtra(EXTRA_PORT).toInt()
            password = intent.getStringExtra(EXTRA_PASSWORD)
        }

        Log.i(TAG, "$nickname/$hostname/$port/$password")

        mChatBanner = findViewById<View>(R.id.chatEnabledBanner) as TextView

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sendButton.setOnClickListener { sendButtonClicked() }
        sayButton.setOnClickListener { sayButtonClicked() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_rcon, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
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
                val rconRepsonseText = findViewById<View>(R.id.rconResponse) as TextView
                rconRepsonseText.text = ""
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
            R.id.action_settings -> {
                val intent = Intent(this@ServerRconActivity, SettingsActivity::class.java)
                startActivity(intent)

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

        mChatBanner!!.visibility = View.GONE

        // Send commands to server to run
        val tempCMDs = arrayOf("logaddress_del $hostname:$port", "log off")
        threadRconRequest(false, tempCMDs)
    }

    private fun enableLogMode() {
        // Real-time logging connection
        ConnectTask().execute("")

        // Get a wakelock so we don't disconnect from the socket
        releaseWakeLock()
        acquireWakeLock()

        mChatBanner!!.visibility = View.VISIBLE

        // Send commands to server to run
        val tempCmds = arrayOf("logaddress_add $hostname:$port", "log off", "log on")
        threadRconRequest(false, tempCmds)
    }

    override fun onDestroy() {
        if (chatModeActive || logModeActive) {
            // Disable chat mode before we exit
            disableLogMode() // NOTE: This also calls releaseWakeLock()
        }

        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sourceservermanager:SSM Logging Lock")
        wakeLock!!.acquire(10*60*1000L /*10 minutes*/)
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

    private fun sendButtonClicked() {
        val rconCommandText = findViewById<View>(R.id.rconCommand) as EditText

        val tempCmd = arrayOf(rconCommandText.text.toString())
        threadRconRequest(false, tempCmd)

        rconCommandText.setText("")
    }

    private fun sayButtonClicked() {
        val rconCommandText = findViewById<View>(R.id.rconCommand) as EditText

        val tempCmd = arrayOf(rconCommandText.text.toString())
        threadRconRequest(true, tempCmd)

        rconCommandText.setText("")
    }

    fun sendRconRequest(command: String) {
        // GO HERE
        // SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        val settings = PreferenceManager
                .getDefaultSharedPreferences(applicationContext)
        try {
            // Check IP/port
            serverResponse = when {
                hostname!!.isEmpty() -> getString(R.string.noIP)
                port == -1 -> getString(R.string.noPort)
                else -> // Call Source (Half Life 2 & others) rcon without local port
                    SourceRcon.send(
                            hostname!!,
                            port,
                            password!!,
                            command,
                            settings.getString("pref_key_rcon_timeout", "5")!!
                    )
            }

        } catch (e: ResponseEmpty) {
            serverResponse = getString(R.string.emptyRcon)
        } catch (e: BadRcon) {
            // Wrong RCON password
            serverResponse = getString(R.string.badRcon)
        } catch (e: IOException) {
            // The socket timed out on HL2 style, try HL1! (inefficient, I know,
            // but I don't want to add anything to server prefs now)
            try {
                // Call HL1 rcon with local port 0
                serverResponse = Rcon.send(
                        0,
                        hostname!!,
                        port,
                        password!!,
                        command,
                        settings.getString("pref_key_rcon_timeout", "5")!!
                )
            } catch (e2: ResponseEmpty) {
                serverResponse = getString(R.string.emptyRcon)
            } catch (e2: SocketTimeoutException) {
                serverResponse = getString(R.string.socketTimeout)
            } catch (e2: BadRcon) {
                // Wrong RCON password
                serverResponse = getString(R.string.badRcon)
            } catch (e2: Exception) {
                // Something else happened...
                serverResponse = getString(R.string.failedRcon)
            }

        } catch (e: Exception) {
            // Something else happened...
            serverResponse = getString(R.string.failedRcon)
        }

    }

    private fun threadRconRequest(isSay: Boolean,
                                  commands: Array<String>): Boolean {
        // final EditText rconCommandText = (EditText)
        // findViewById(R.id.rconCommand);

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
            mTcpClient!!.run("$hostname:$port")

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
                    serverResponse = getString(R.string.remote_disconnect_warning)
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
