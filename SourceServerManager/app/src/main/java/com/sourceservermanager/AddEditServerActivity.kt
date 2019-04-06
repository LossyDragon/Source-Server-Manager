package com.sourceservermanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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
            nickname.setText(intent.getStringExtra(EXTRA_TITLE))
            address.setText(intent.getStringExtra(EXTRA_IP))
            port.setText(intent.getStringExtra(EXTRA_PORT))
            password.setText(intent.getStringExtra(EXTRA_PASSWORD))

            title = if (nickname.text.toString().isBlank())
                String.format(resources.getString(R.string.title_edit_activity), address.text)
            else
                String.format(resources.getString(R.string.title_edit_activity), nickname.text)

            add_server_button.text = resources.getString(R.string.button_update)
        } else {
            title = resources.getString(R.string.title_add_activity)
            add_server_button.text = resources.getString(R.string.button_add)
        }

        add_server_button.setOnClickListener { saveServer() }

        show_password.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                password.transformationMethod = PasswordTransformationMethod.getInstance()
                password.setSelection(password.text!!.length)

            } else {
                password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                password.setSelection(password.text!!.length)
            }
        }
    }

    private fun saveServer() {

        val serverAddress: String = address.text.toString()
        val serverPort: String = port.text.toString()

        address.error = null
        port.error = null

        //Check if address is empty.
        if (serverAddress.trim().isBlank()) {
            address.error = "Cannot be empty to save"
            address.requestFocus()
            return
        }

        //Check if port is empty
        if (serverPort.trim().isBlank()) {
            port.error = "Cannot be empty to save"
            port.requestFocus()
            return
        }

        //Check if port is valid
        if (serverPort.toInt() !in 1..65535) {
            port.error = "Not a valid port"
            port.requestFocus()
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