package com.sourceservermanager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sourceservermanager.data.Server
import kotlinx.android.synthetic.main.activity_server_list.*

class ServerListActivity : AppCompatActivity() {

    companion object {
        const val ADD_SERVER_REQUEST = 1
        const val EDIT_SERVER_REQUEST = 2

        private const val INTERNET_PERMISSION_REQUEST = 100
    }

    private lateinit var serverViewModel: ServerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_list)

        //setSupportActionBar(toolbar)
        setTitle(R.string.title_server_list)

        add_server_fab.setOnClickListener {
            startActivityForResult(
                    Intent(this, AddEditServerActivity::class.java),
                    ADD_SERVER_REQUEST
            )
        }

        recycler_view.layoutManager = LinearLayoutManager(this@ServerListActivity)
        recycler_view.setHasFixedSize(true)

        val adapter = ServerAdapter()
        recycler_view.adapter = adapter

        serverViewModel = ViewModelProviders.of(this@ServerListActivity).get(ServerViewModel::class.java)

        serverViewModel.getAllServers().observe(this@ServerListActivity, Observer<List<Server>>{
            adapter.submitList(it)
        })

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT.or(ItemTouchHelper.RIGHT)) {
            override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                serverViewModel.delete(adapter.getServerAt(viewHolder.adapterPosition))
                Toast.makeText(baseContext,
                        adapter.getServerAt(viewHolder.adapterPosition).serverTitle + " deleted.", Toast.LENGTH_SHORT).show()
            }
        }).attachToRecyclerView(recycler_view)

        //Long click listener to edit a server
        adapter.setOnItemLongClickListener(object : ServerAdapter.OnItemLongClickListener {
            override fun onItemLongClick(server: Server) {
                val intent = Intent(baseContext, AddEditServerActivity::class.java)
                intent.putExtra(AddEditServerActivity.EXTRA_ID, server.id)
                intent.putExtra(AddEditServerActivity.EXTRA_TITLE, server.serverTitle)
                intent.putExtra(AddEditServerActivity.EXTRA_IP, server.serverIP)
                intent.putExtra(AddEditServerActivity.EXTRA_PORT, server.serverPort)
                intent.putExtra(AddEditServerActivity.EXTRA_PASSWORD, server.serverPassword)
                startActivityForResult(intent, EDIT_SERVER_REQUEST)
            }
        })

        //Click listener to launch a server for Rcon control
        adapter.setOnItemClickListener(object : ServerAdapter.OnItemClickListener {
            override fun onItemClick(server: Server) {
                val intent = Intent(baseContext, ServerRconActivity::class.java)
                intent.putExtra(ServerRconActivity.EXTRA_ID, server.id)
                intent.putExtra(ServerRconActivity.EXTRA_TITLE, server.serverTitle)
                intent.putExtra(ServerRconActivity.EXTRA_IP, server.serverIP)
                intent.putExtra(ServerRconActivity.EXTRA_PORT, server.serverPort)
                intent.putExtra(ServerRconActivity.EXTRA_PASSWORD, server.serverPassword)
                startActivity(intent)
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_server_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this@ServerListActivity, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_add_server -> {
                val intent = Intent(this@ServerListActivity, AddEditServerActivity::class.java)
                startActivity(intent)
                return true
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
                    data!!.getStringExtra(AddEditServerActivity.EXTRA_TITLE),
                    data.getStringExtra(AddEditServerActivity.EXTRA_IP),
                    data.getStringExtra(AddEditServerActivity.EXTRA_PORT),
                    data.getStringExtra(AddEditServerActivity.EXTRA_PASSWORD)
            )
            serverViewModel.insert(newServer)

            Toast.makeText(this, "Server saved!", Toast.LENGTH_SHORT).show()
        } else if (requestCode == EDIT_SERVER_REQUEST && resultCode == Activity.RESULT_OK) {
            val id = data?.getIntExtra(AddEditServerActivity.EXTRA_ID, -1)

            if (id == -1) {
                Toast.makeText(this, "Could not update! Error!", Toast.LENGTH_SHORT).show()
            }

            val updateServer = Server(
                    data!!.getStringExtra(AddEditServerActivity.EXTRA_TITLE),
                    data.getStringExtra(AddEditServerActivity.EXTRA_IP),
                    data.getStringExtra(AddEditServerActivity.EXTRA_PORT),
                    data.getStringExtra(AddEditServerActivity.EXTRA_PASSWORD)
            )
            updateServer.id = data.getIntExtra(AddEditServerActivity.EXTRA_ID, -1)
            serverViewModel.update(updateServer)
            Toast.makeText(this, "Server saved!", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(this, "Server not saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doPermissionsCheck() {
        if (ContextCompat.checkSelfPermission(this@ServerListActivity,
                        Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this@ServerListActivity,
                        Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this@ServerListActivity,
                        Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this@ServerListActivity,
                        Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this@ServerListActivity,
                    arrayOf(
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK),
                    INTERNET_PERMISSION_REQUEST)

        }
    }
}