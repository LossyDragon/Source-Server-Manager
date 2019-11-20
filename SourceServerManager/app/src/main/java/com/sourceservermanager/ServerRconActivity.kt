package com.sourceservermanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sourceservermanager.data.Rcon
import com.sourceservermanager.rcon.GoldRconConnection
import com.sourceservermanager.rcon.SourceRconConnection
import com.sourceservermanager.rcon.exception.*
import kotlinx.android.synthetic.main.activity_server_rcon.*
import java.text.SimpleDateFormat
import java.util.*

class ServerRconActivity : AppCompatActivity(), RconAdapter.OnItemLongClickListener {

    companion object {
        private val TAG = ServerRconActivity::class.java.simpleName

        const val EXTRA_ID = "com.sourceservermanager.EXTRA_ID"
        const val EXTRA_TITLE = "com.sourceservermanager.EXTRA_TITLE"
        const val EXTRA_IP = "com.sourceservermanager.EXTRA_IP"
        const val EXTRA_PORT = "com.sourceservermanager.EXTRA_PORT"
        const val EXTRA_PASSWORD = "com.sourceservermanager.EXTRA_PASSWORD"
        const val EXTRA_ISGOLDSOURCE = "com.sourceservermanager.EXTRA_ISGOLDSOURCE"
        const val EXTRA_CV_PORT = "com.sourceservermanager.EXTRA_CV_PORT"
        const val EXTRA_CV_PASSWORD = "com.sourceservermanager.EXTRA_CV_PASSWORD"
    }

    //Server Variables
    private var id: String? = null
    private var nickname: String? = null
    private var address: String? = null
    private var port: String? = null
    private var password: String? = null
    private var isGoldSource: Boolean? = null
    private var checkValvePort: String? = null
    private var checkValvePassword: String? = null

    private var serverResponse: String? = null

    private lateinit var rconAdapter: RconAdapter

    private var sourceConnection: SourceRconConnection? = null
    private lateinit var rconViewModel: RconViewModel

    internal val mHandler = Handler()
    private val scrollHandler = Handler()

    private lateinit var autocomplete: Array<String>

    // Create runnable for scrolling to bottom on scrollview
    private val scrollBottom: Runnable = Runnable {
        // Force scroll to scroll to the bottom
        recycler_rcon_view.scrollToPosition(rconAdapter.itemCount - 1)
    }

    // Create runnable for posting server response from thread
    internal val mUpdateResults: Runnable = Runnable {
        if (serverResponse != null) {

            rconViewModel.insert(rcon = Rcon(
                    nickname!!,
                    address!!,
                    serverResponse!!,
                    getTime()
            ))

            Log.i(TAG, "Server Response: $serverResponse")

            //rconResponse.append(serverResponse)
            // Force scroll to scroll to the bottom
            scrollHandler.postDelayed(scrollBottom, 10)

            serverResponse = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_rcon)

        autocomplete = SettingsActivity().readSharedPrefs(this).split(",").toTypedArray()

        for (i in autocomplete)
            Log.d("AutoComplete", i)

        if (intent.hasExtra(EXTRA_ID)) {
            nickname = intent.getStringExtra(EXTRA_TITLE)
            address = intent.getStringExtra(EXTRA_IP)
            port = intent.getStringExtra(EXTRA_PORT)
            password = intent.getStringExtra(EXTRA_PASSWORD)
            isGoldSource = intent.getBooleanExtra(EXTRA_ISGOLDSOURCE, false)
            checkValvePort = intent.getStringExtra(EXTRA_CV_PORT)
            checkValvePassword = intent.getStringExtra(EXTRA_CV_PASSWORD)
        } else {
            Log.w(TAG, "Intent has no data")
            Toast.makeText(this, "Intent has no data", Toast.LENGTH_LONG).show()
            finish()
        }

        Log.d(TAG, "$nickname/$address/$port/$password")

        title = if (nickname!!.isBlank())
            String.format(resources.getString(R.string.title_rcon_activity), address)
        else
            String.format(resources.getString(R.string.title_rcon_activity), nickname)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sendButton.setOnClickListener { buttonClicked(false) }
        sayButton.setOnClickListener { buttonClicked(true) }

        button_more.setOnClickListener {
            val popup = PopupMenu(this, it)
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

        rconCommand.setAdapter(
                ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        autocomplete
                )
        )

        rconCommand.setOnClickListener { scrollHandler.postDelayed(scrollBottom, 10) }

        //We don't know what command we're trying to do when KEYCODE_ENTER is pressed, so let's not do this yet
        //rconCommand.setOnKeyListener { _, keyCode, keyEvent ->
        //    if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
        //        buttonClicked()
        //        return@setOnKeyListener true
        //    }
        //    return@setOnKeyListener false
        //}

        rconAdapter = RconAdapter(this)
        recycler_rcon_view.apply {
            layoutManager = LinearLayoutManager(this@ServerRconActivity)
            adapter = rconAdapter
            setHasFixedSize(true)
        }

        rconViewModel = ViewModelProvider(this).get(RconViewModel::class.java)
        rconViewModel.getRconHistory(address!!).observe(this, Observer<List<Rcon>> {
            rconAdapter.submitList(it)
            scrollHandler.postDelayed(scrollBottom, 10)
        })
    }

    override fun onItemLongClick(rcon: Rcon) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        val clip: ClipData = ClipData.newPlainText(getString(R.string.clipboard_primary), rcon.rconMessage)
        clipboard?.setPrimaryClip(clip)

        Toast.makeText(this, getString(R.string.toast_message_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        sourceConnection?.close()
        sourceConnection = null
    }

    override fun onDestroy() {
        super.onDestroy()

        nickname = null
        address = null
        port = null
        password = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_rcon, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_chat -> {
                //Go to the Chat Activity, powered by CheckValve
                val intent = Intent(baseContext, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_ID, id)
                    putExtra(ChatActivity.EXTRA_TITLE, nickname)
                    putExtra(ChatActivity.EXTRA_IP, address)
                    putExtra(ChatActivity.EXTRA_PORT, port)
                    putExtra(ChatActivity.EXTRA_PASSWORD, password)
                    putExtra(ChatActivity.EXTRA_ISGOLDSOURCE, isGoldSource)
                    putExtra(ChatActivity.EXTRA_CV_PORT, checkValvePort)
                    putExtra(ChatActivity.EXTRA_CV_PASSWORD, checkValvePassword)
                }
                startActivity(intent)
            }
            R.id.action_clear_log -> {
                //Clear the RCON log
                AlertDialog.Builder(this).apply {
                    setTitle(getString(R.string.dialog_delete_rcon))
                    setMessage(getString(R.string.dialog_delete_rcon_message))
                    setPositiveButton(getString(R.string.dialog_delete_delete)) { _, _ ->
                        rconViewModel.deleteRconHistory(address!!)
                    }
                    setNegativeButton(getString(R.string.dialog_delete_cancel)) { _, _ -> }
                }.show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun buttonClicked(isSay: Boolean) {
        threadRconRequest(isSay, arrayOf(rconCommand.text.toString()))
        rconCommand.setText("")
    }

    private fun sendRconRequest(command: String) {

        serverResponse = try {

            if (isGoldSource!!) {
                GoldRconConnection().send(0, address!!, port!!.toInt(), password!!, command, "5")
            } else {
                if (sourceConnection == null)
                    sourceConnection = SourceRconConnection(address!!, port!!.toInt(), password!!)

                sourceConnection!!.send(command)
            }

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
                if (isSay) {
                    for (command in commands) {
                        sendRconRequest("say $command")
                    }
                } else {
                    for (command in commands) {
                        if (command.length >= 3) {
                            sendRconRequest(command)
                        }
                    }
                }

                mHandler.post(mUpdateResults)
            }
        }
        t.start()
        return true
    }

    private fun getTime(): String {
        val sdf = SimpleDateFormat("MM/dd/yyyy - HH:mm:ss", Locale.ENGLISH)
        val date = Date()
        return sdf.format(date).toString()
    }
}
