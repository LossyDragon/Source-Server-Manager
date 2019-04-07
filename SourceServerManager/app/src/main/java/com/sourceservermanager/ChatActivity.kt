package com.sourceservermanager

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.sourceservermanager.data.Chat
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ChatActivity"

        const val EXTRA_ID = "com.sourceservermanager.EXTRA_ID"
        const val EXTRA_TITLE = "com.sourceservermanager.EXTRA_TITLE"
        const val EXTRA_IP = "com.sourceservermanager.EXTRA_IP"
        const val EXTRA_PORT = "com.sourceservermanager.EXTRA_PORT"
        const val EXTRA_PASSWORD = "com.sourceservermanager.EXTRA_PASSWORD"
        const val EXTRA_CV_PORT = "com.sourceservermanager.EXTRA_CV_PORT"
        const val EXTRA_CV_PASSWORD = "com.sourceservermanager.EXTRA_CV_PASSWORD"
    }

    private var nickname: String? = null
    private var address: String? = null
    private var port: String? = null
    private var password: String? = null
    private var checkValvePort: String? = null
    private var checkValvePassword: String? = null

    private lateinit var chatViewModel: ChatViewModel
    //TODO add UNDO revert?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        supportActionBar?.setHomeButtonEnabled(true)

        if (intent.hasExtra(EXTRA_ID)) {
            nickname = intent.getStringExtra(EXTRA_TITLE)
            address = intent.getStringExtra(EXTRA_IP)
            port = intent.getStringExtra(EXTRA_PORT)
            password = intent.getStringExtra(EXTRA_PASSWORD)
            checkValvePort = intent.getStringExtra(EXTRA_CV_PORT)
            checkValvePassword = intent.getStringExtra(EXTRA_CV_PASSWORD)
        }

        title = "Chat: $nickname"

        recycler_chat_view.layoutManager = LinearLayoutManager(this@ChatActivity)
        recycler_chat_view.setHasFixedSize(true)

        val adapter = ChatAdapter()
        recycler_chat_view.adapter = adapter

        chatViewModel = ViewModelProviders.of(this@ChatActivity).get(ChatViewModel::class.java)

        chatViewModel.getAllChats().observe(this@ChatActivity, Observer<List<Chat>> {
            adapter.submitList(it)
        })

        adapter.setOnItemClickListener(object : ChatAdapter.OnItemClickListener {
            override fun onItemClick(chat: Chat) {
                //TODO add COPY TEXT TO CLIPBOARD
            }
        })

        //TODO shit
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId) {
            R.id.action_chat_delete -> {
                //TODO -> Show confirmation dialog before delete
                //TODO -> Delete all chats.
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
