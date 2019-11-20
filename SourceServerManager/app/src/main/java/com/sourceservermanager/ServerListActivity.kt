package com.sourceservermanager

/**
 *
 * Credits:
 * SSM Application: https://github.com/ViXXoR/Source-Server-Manager
 * RCON Code: https://github.com/riking/srcds-controller (And code from ViXXoR)
 * MVVM Example: https://github.com/berkeatac/Notes-App
 * CheckValve: https://github.com/daparker/checkvalve
 *
 */

import android.Manifest.permission.*
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.sourceservermanager.data.Server
import kotlinx.android.synthetic.main.activity_server_list.*

class ServerListActivity : AppCompatActivity(), ServerAdapter.OnItemClickListener, ServerAdapter.OnItemLongClickListener {

    companion object {
        const val ADD_SERVER_REQUEST = 1
        const val EDIT_SERVER_REQUEST = 2

        private const val INTERNET_PERMISSION_REQUEST = 100
    }

    private lateinit var serverViewModel: ServerViewModel
    private var recentlyDeletedItem: Server? = null
    private lateinit var serverAdapter: ServerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_list)

        title = resources.getString(R.string.title_server_activity)

        serverAdapter = ServerAdapter(this, this)

        add_server_fab.setOnClickListener {
            startActivityForResult(
                    Intent(this, AddEditServerActivity::class.java),
                    ADD_SERVER_REQUEST
            )
        }

        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@ServerListActivity)
            adapter = serverAdapter
            setHasFixedSize(true)
        }

        serverViewModel = ViewModelProvider(this).get(ServerViewModel::class.java)
        serverViewModel.getAllServers().observe(this, Observer<List<Server>> {
            serverAdapter.submitList(it)
        })

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            private var icon: Drawable? = null
            private var background: ColorDrawable? = null

            override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    icon = resources.getDrawable(R.drawable.ic_delete)
                    background = ColorDrawable(resources.getColor(R.color.colorAccent))
                } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    icon = resources.getDrawable(R.drawable.ic_delete, null)
                    background = ColorDrawable(resources.getColor(R.color.colorAccent, null))
                }

                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 20

                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon!!.intrinsicHeight) / 2
                val iconBottom = iconTop + icon!!.intrinsicHeight

                when {
                    dX < 0 -> {
                        val iconLeft = itemView.right - iconMargin - icon!!.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        icon!!.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                        background!!.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset,
                                itemView.top, itemView.right, itemView.bottom)
                    }
                    else ->
                        background!!.setBounds(0, 0, 0, 0)
                }

                background!!.draw(c)
                icon!!.draw(c)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                recentlyDeletedItem = serverAdapter.getServerAt(viewHolder.adapterPosition)
                serverViewModel.delete(serverAdapter.getServerAt(viewHolder.adapterPosition))

                val snack = Snackbar.make(
                        server_list_activity,
                        recentlyDeletedItem!!.serverTitle + getString(R.string.snackbar_server_deleted),
                        15000
                )
                snack.setAction(R.string.snackbar_undo) {
                    serverViewModel.insert(recentlyDeletedItem!!)
                    recentlyDeletedItem = null
                }
                snack.show()

            }
        }).attachToRecyclerView(recycler_view)
    }

    override fun onItemClick(server: Server) {
        val intent = Intent(baseContext, ServerRconActivity::class.java).apply {
            putExtra(ServerRconActivity.EXTRA_ID, server.id)
            putExtra(ServerRconActivity.EXTRA_TITLE, server.serverTitle)
            putExtra(ServerRconActivity.EXTRA_IP, server.serverIP)
            putExtra(ServerRconActivity.EXTRA_PORT, server.serverPort)
            putExtra(ServerRconActivity.EXTRA_PASSWORD, server.serverPassword)
            putExtra(ServerRconActivity.EXTRA_ISGOLDSOURCE, server.isGoldSource)
            putExtra(ServerRconActivity.EXTRA_CV_PORT, server.checkValvePort)
            putExtra(ServerRconActivity.EXTRA_CV_PASSWORD, server.checkValvePassword)
        }
        startActivity(intent)
    }

    override fun onItemLongClick(server: Server) {
        val intent = Intent(baseContext, AddEditServerActivity::class.java).apply {
            putExtra(AddEditServerActivity.EXTRA_ID, server.id)
            putExtra(AddEditServerActivity.EXTRA_TITLE, server.serverTitle)
            putExtra(AddEditServerActivity.EXTRA_IP, server.serverIP)
            putExtra(AddEditServerActivity.EXTRA_PORT, server.serverPort)
            putExtra(AddEditServerActivity.EXTRA_PASSWORD, server.serverPassword)
            putExtra(AddEditServerActivity.EXTRA_ISGOLDSOURCE, server.isGoldSource)
            putExtra(AddEditServerActivity.EXTRA_CV_PORT, server.checkValvePort)
            putExtra(AddEditServerActivity.EXTRA_CV_PASSWORD, server.checkValvePassword)
        }
        startActivityForResult(intent, EDIT_SERVER_REQUEST)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_server_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(baseContext, SettingsActivity::class.java))
            R.id.action_delete_servers -> {
                AlertDialog.Builder(this).apply {
                    setCancelable(true)
                    setTitle(getString(R.string.dialog_delete_server_all))
                    setMessage(getString(R.string.dialog_delete_server_all_message))
                    setPositiveButton(getString(R.string.dialog_delete_delete)) { _, _ ->
                        serverViewModel.deleteAllServers()
                    }
                    setNegativeButton(getString(R.string.dialog_delete_cancel)) { _, _ -> }
                }.show()
            }
            R.id.action_delete_rcons -> {
                AlertDialog.Builder(this).apply {
                    setCancelable(true)
                    setTitle(getString(R.string.dialog_delete_rcon_all))
                    setMessage(getString(R.string.dialog_delete_rcon_all_message))
                    setPositiveButton(getString(R.string.dialog_delete_delete)) { _, _ ->
                        serverViewModel.deleteAllRcon()
                    }
                    setNegativeButton(getString(R.string.dialog_delete_cancel)) { _, _ -> }
                }.show()
            }
            R.id.action_delete_chats -> {
                AlertDialog.Builder(this).apply {
                    setCancelable(true)
                    setTitle(getString(R.string.dialog_delete_chat_all))
                    setMessage(getString(R.string.dialog_delete_chat_all_message))
                    setPositiveButton(getString(R.string.dialog_delete_delete)) { _, _ ->
                        serverViewModel.deleteAllChats()
                    }
                    setNegativeButton(getString(R.string.dialog_delete_cancel)) { _, _ -> }
                }.show()
            }
            R.id.action_help -> {
                //val intent = Intent(this, HelpActivity::class.java)
                //startActivity(intent)
                //return true
                //TODO add basic help activity
                Toast.makeText(baseContext, "TODO: HELP Activity", Toast.LENGTH_LONG).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        doPermissionsCheck()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_SERVER_REQUEST && resultCode == Activity.RESULT_OK) {
            val newServer = Server(
                    serverTitle = data!!.getStringExtra(AddEditServerActivity.EXTRA_TITLE)!!,
                    serverIP = data.getStringExtra(AddEditServerActivity.EXTRA_IP)!!,
                    serverPort = data.getStringExtra(AddEditServerActivity.EXTRA_PORT)!!,
                    serverPassword = data.getStringExtra(AddEditServerActivity.EXTRA_PASSWORD)!!,
                    isGoldSource = data.getBooleanExtra(AddEditServerActivity.EXTRA_ISGOLDSOURCE, false),
                    checkValvePort = data.getStringExtra(AddEditServerActivity.EXTRA_CV_PORT)!!,
                    checkValvePassword = data.getStringExtra(AddEditServerActivity.EXTRA_CV_PASSWORD)!!
            )
            serverViewModel.insert(newServer)

            Snackbar.make(server_list_activity, resources.getString(R.string.snackbar_saved), Snackbar.LENGTH_SHORT).show()
        } else if (requestCode == EDIT_SERVER_REQUEST && resultCode == Activity.RESULT_OK) {
            val id = data?.getIntExtra(AddEditServerActivity.EXTRA_ID, -1)

            if (id == -1) {
                Snackbar.make(server_list_activity, resources.getString(R.string.snackbar_update_error), 8000).show()
            }

            val updateServer = Server(
                    serverTitle = data!!.getStringExtra(AddEditServerActivity.EXTRA_TITLE)!!,
                    serverIP = data.getStringExtra(AddEditServerActivity.EXTRA_IP)!!,
                    serverPort = data.getStringExtra(AddEditServerActivity.EXTRA_PORT)!!,
                    serverPassword = data.getStringExtra(AddEditServerActivity.EXTRA_PASSWORD)!!,
                    isGoldSource = data.getBooleanExtra(AddEditServerActivity.EXTRA_ISGOLDSOURCE, false),
                    checkValvePort = data.getStringExtra(AddEditServerActivity.EXTRA_CV_PORT)!!,
                    checkValvePassword = data.getStringExtra(AddEditServerActivity.EXTRA_CV_PASSWORD)!!
            )
            updateServer.id = data.getIntExtra(AddEditServerActivity.EXTRA_ID, -1)
            serverViewModel.update(updateServer)
            Snackbar.make(server_list_activity, getString(R.string.snackbar_saved), Snackbar.LENGTH_SHORT).show()

        } else {
            Snackbar.make(server_list_activity, getString(R.string.snackbar_not_saved), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun doPermissionsCheck() {
        if (ContextCompat.checkSelfPermission(this,
                        INTERNET) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        ACCESS_NETWORK_STATE) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        ACCESS_WIFI_STATE) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        WAKE_LOCK) != PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, WAKE_LOCK), INTERNET_PERMISSION_REQUEST)

        }
    }
}