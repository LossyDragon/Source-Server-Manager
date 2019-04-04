package com.sourceservermanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_add_server.*

class AddEditServerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "com.sourceservermanager.EXTRA_ID"
        const val EXTRA_TITLE = "com.sourceservermanager.EXTRA_TITLE"
        const val EXTRA_IP = "com.sourceservermanager.EXTRA_IP"
        const val EXTRA_PORT = "com.sourceservermanager.EXTRA_PORT"
        const val EXTRA_PASSWORD = "com.sourceservermanager.EXTRA_PASSWORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_server)

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        if (intent.hasExtra(EXTRA_ID)) {
            title = resources.getString(R.string.title_update)
            nickname.setText(intent.getStringExtra(EXTRA_TITLE))
            address.setText(intent.getStringExtra(EXTRA_IP))
            port.setText(intent.getStringExtra(EXTRA_PORT))
            password.setText(intent.getStringExtra(EXTRA_PASSWORD))
            add_server_button.text = resources.getString(R.string.button_update)
        } else {
            title = resources.getString(R.string.title_add)
        }

        add_server_button.setOnClickListener { saveNote() }
    }

    private fun saveNote() {

        if (address.text.toString().trim().isBlank() || port.text.toString().trim().isBlank()) {
            Toast.makeText(this, "Can not insert empty server!", Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent().apply {
            putExtra(EXTRA_TITLE, nickname.text.toString())
            putExtra(EXTRA_IP, address.text.toString())
            putExtra(EXTRA_PORT, port.text.toString())
            putExtra(EXTRA_PASSWORD, password.text.toString())

            if (intent.getIntExtra(EXTRA_ID, -1) != -1)
                putExtra(EXTRA_ID, intent.getIntExtra(EXTRA_ID, -1))
        }

        setResult(Activity.RESULT_OK, data)
        finish()
    }
}