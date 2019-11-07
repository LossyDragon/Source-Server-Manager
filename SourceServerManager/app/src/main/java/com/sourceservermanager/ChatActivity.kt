package com.sourceservermanager

/**
 * Parts of code sampled/borrowed from CheckValve/ChatViewerActivity.java
 * by David A. Parker
 */

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sourceservermanager.checkvalve.ChatRunnable
import com.sourceservermanager.checkvalve.NetworkEventReceiver
import com.sourceservermanager.data.Chat
import com.sourceservermanager.rcon.GoldRconConnection
import com.sourceservermanager.rcon.SourceRconConnection
import com.sourceservermanager.rcon.exception.*
import kotlinx.android.synthetic.main.activity_chat.*
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*


//This is a rough cut to understand the workings of CV Chat Relay.
class ChatActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ChatActivity"

        const val EXTRA_ID = "com.sourceservermanager.EXTRA_ID"
        const val EXTRA_TITLE = "com.sourceservermanager.EXTRA_TITLE"
        const val EXTRA_IP = "com.sourceservermanager.EXTRA_IP"
        const val EXTRA_PORT = "com.sourceservermanager.EXTRA_PORT"
        const val EXTRA_PASSWORD = "com.sourceservermanager.EXTRA_PASSWORD"
        const val EXTRA_ISGOLDSOURCE = "com.sourceservermanager.EXTRA_ISGOLDSOURCE"
        const val EXTRA_CV_PORT = "com.sourceservermanager.EXTRA_CV_PORT"
        const val EXTRA_CV_PASSWORD = "com.sourceservermanager.EXTRA_CV_PASSWORD"
    }

    private var nickname: String? = null
    private var address: String? = null
    private var port: String? = null
    private var password: String? = null
    private var isGoldSource: Boolean? = null
    private var checkValvePort: String? = null
    private var checkValvePassword: String? = null

    private var sourceConnection: SourceRconConnection? = null
    private lateinit var adapter: ChatAdapter

    //Chat Stuff
    private var receiverRunnable: NetworkEventReceiver? = null
    private var chatThread: Thread? = null
    private var receiverThread: Thread? = null
    private var chatRunnable: ChatRunnable? = null
    private lateinit var chatViewModel: ChatViewModel

    private val scrollHandler = Handler()

    // Create runnable for scrolling to bottom on scrollview
    private val scrollBottom: Runnable = Runnable {
        // Force scroll to scroll to the bottom
        recycler_chat_view.scrollToPosition(adapter.itemCount - 1)
    }

    /*
     * Message object "what" codes:
     * -2   = An exception during shutdown (maybe normal)
     * -1   = Failed to connect to the chat relay (probably a SocketException)
     *  1   = A heartbeat was received from the server
     *  3   = Connection failure (includes error message as String object)
     *  4   = Connection successful
     *  5   = Chat message (includes ChatMessage object)
     *  255 = Disconnected (probably due to shutdown)
     *
     *  Values 1-5 correspond to response types sent from the Chat Relay
     */
    //TODO: HandlerLeak
    private val chatClientHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                -2 -> {
                    finish()
                }
                -1 -> {
                    postSystemMessage("Failed to connect, possibly...")
                }
                1 -> { /*Nothing - HeartBeat*/
                }
                3 -> {
                    postSystemMessage("Connection Refused")
                }
                4 -> {
                    postSystemMessage("Connection successful")
                }
                5 -> {
                    val chatMessage = msg.obj as Chat

                    chatViewModel.insert(chat = Chat(
                            chatMessage.protocolVersion,
                            chatMessage.sayTeamFlag,
                            chatMessage.serverTimestamp,
                            chatMessage.gameServerIP,
                            chatMessage.gameServerPort,
                            getTime(), //Get device's time
                            chatMessage.playerName,
                            chatMessage.playerTeam,
                            chatMessage.message
                    ))
                }
                255 -> {
                    postSystemMessage("Disconnected!")
                }
                else -> {
                    postSystemMessage("Handler received an unexpected value (" + msg.what + ")")
                }
            }
        }
    }

    /*
     * Message object "what" codes:
     * -2  =  Fatal exception in the NetworkEventReceiver thread
     * -1  =  No network connectivity
     *  0  =  Initial event from broadcast receiver (should be ignored)
     *  1  =  Network connection change
     */
    //TODO: HandlerLeak
    private val networkEventHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            Log.i(TAG, "Received " + msg.what + " from NetworkEventReceiver")

            when (msg.what) {
                -2 -> {
                    Toast.makeText(this@ChatActivity, "The network event receiver has aborted", Toast.LENGTH_LONG).show()
                    finish()
                }
                -1 -> {
                    postSystemMessage("Connection has been lost")
                    //Toast.makeText(this@ChatActivity, "Connection has been lost", Toast.LENGTH_LONG).show()
                    shutDownChatRelay()
                }
                0 -> {
                    /* Ignored */
                }
                1 -> {
                    try {
                        getChatRelayConnection()
                    } catch (e: UnknownHostException) {
                        postSystemMessage("UnknownHostException")
                        //Toast.makeText(this@ChatActivity, "UnknownHostException", Toast.LENGTH_LONG).show()
                        Log.w(TAG, "Unknown Host Ex: " + e.localizedMessage)
                    }
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        supportActionBar?.setHomeButtonEnabled(true)

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

        title = String.format(getString(R.string.title_chat_activity), nickname)

        recycler_chat_view.layoutManager = LinearLayoutManager(this@ChatActivity)
        recycler_chat_view.setHasFixedSize(true)

        adapter = ChatAdapter()
        recycler_chat_view.adapter = adapter

        chatViewModel = ViewModelProvider(this@ChatActivity).get(ChatViewModel::class.java)

        chatViewModel.getChatHistory(address!!).observe(this@ChatActivity, Observer<List<Chat>> {
            adapter.submitList(it)

            scrollHandler.postDelayed(scrollBottom, 10)
        })

        adapter.setOnItemLongClickListener(object : ChatAdapter.OnItemLongClickListener {
            override fun onItemLongClick(chat: Chat) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
                val clip: ClipData = ClipData.newPlainText(getString(R.string.clipboard_primary), chat.message)
                clipboard?.setPrimaryClip(clip)

                Toast.makeText(this@ChatActivity, getString(R.string.toast_message_copied), Toast.LENGTH_SHORT).show()
            }
        })

        chat_send.setOnClickListener { buttonClicked() }

        chat_box.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                buttonClicked()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        chat_box.setOnClickListener { scrollHandler.postDelayed(scrollBottom, 10) }

        receiverRunnable = NetworkEventReceiver(this@ChatActivity, networkEventHandler)

        if (receiverRunnable == null) {
            Log.e(TAG, "receiverRunnable and cannot continue!")
            finish()
        } else {
            receiverThread = Thread(receiverRunnable)

            if (receiverThread == null) {
                Log.e(TAG, "receiverThread and cannot continue!")
                finish()
            }

            receiverThread!!.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_chat_delete -> {
                val builder = AlertDialog.Builder(this@ChatActivity)
                        .setTitle(getString(R.string.dialog_delete_chat))
                        .setMessage(getString(R.string.dialog_delete_chat_message))
                        .setPositiveButton(
                                resources.getString(R.string.dialog_delete_delete)) { _, _ ->
                            chatViewModel.deleteChatHistory(address!!)
                        }
                        .setNegativeButton(
                                resources.getString(R.string.dialog_delete_cancel)) { _, _ ->

                        }

                val dialog = builder.create()
                dialog.show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        shutDownChatRelay()
        shutDownNetworkReceiver()
    }

    override fun onPause() {
        super.onPause()
        sourceConnection?.close()
        sourceConnection = null
    }

    private fun buttonClicked() {
        threadRconRequest(chat_box.text.toString())
        chat_box.setText("")
        scrollHandler.postDelayed(scrollBottom, 10)
    }

    private fun getTime(): String {
        val sdf = SimpleDateFormat("MM/dd/yyyy - HH:mm:ss", Locale.ENGLISH)
        return sdf.format(Date()).toString()
    }

    private fun threadRconRequest(command: String) {
        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        val t = object : Thread() {
            override fun run() {
                sendRconChat("say $command")
            }
        }
        t.start()
    }

    private fun postSystemMessage(response: String) {

        chatViewModel.insert(chat = Chat(
                0,
                -18,
                0,
                "",
                "",
                getTime(),
                getString(R.string.activity_chat_system_message),
                "",
                response
        ))

        scrollHandler.postDelayed(scrollBottom, 10)
    }

    private fun sendRconChat(command: String) {

        try {
            if (isGoldSource!!) {
                GoldRconConnection().send(0, address!!, port!!.toInt(), password!!, command, "5")
            } else {
                if (sourceConnection == null)
                    sourceConnection = SourceRconConnection(address!!, port!!.toInt(), password!!)

                sourceConnection!!.send(command)
            }

        } catch (e: NotOnlineException) {
            postSystemMessage(getString(R.string.error_not_online))
        } catch (e: AuthenticationException) {
            postSystemMessage(getString(R.string.error_bad_rcon))
        } catch (e: ConnectException) {
            postSystemMessage(getString(R.string.error_failed_rcon))
        } catch (e: ResponseEmptyException) {
            postSystemMessage(getString(R.string.error_empty_rcon))
        } catch (e: TimeoutException) {
            postSystemMessage(getString(R.string.error_socket_timeout))
        }
    }

    //region [REGION] Chat Relay stuff
    fun getChatRelayConnection() {
        // Kill the current connection
        shutDownChatRelay()

        try {

            Log.d(TAG, "ChatRunnable: $address/$checkValvePort/$checkValvePassword/$port/$chatClientHandler")

            chatRunnable = ChatRunnable(
                    address!!,
                    checkValvePort!!,
                    checkValvePassword!!,
                    address!!,
                    port!!,
                    chatClientHandler
            )

            chatThread = Thread(chatRunnable)

            chatThread?.start()
        } catch (e: UnknownHostException) {
            Log.w(TAG, e.localizedMessage!!)
        }
    }

    private fun shutDownChatRelay() {

        //Check the chatRunnable
        chatRunnable?.shutDown()

        //Check the chatThread
        if (chatThread != null && chatThread?.isAlive!!)
            chatThread?.interrupt()

    }

    private fun shutDownNetworkReceiver() {

        receiverRunnable?.unregisterReceiver()
        receiverRunnable?.shutDown()

        if (receiverThread!!.isAlive)
            receiverThread?.interrupt()
    }
    //endregion
}
