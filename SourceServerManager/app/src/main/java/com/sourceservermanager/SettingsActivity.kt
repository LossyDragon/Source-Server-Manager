package com.sourceservermanager

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val RCON_AUTOFILL = "rcon_autofill"
        val defaultAutoFill = arrayOf("kick", "changelevel", "say", "status")
    }

    private var sharedPref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        title = getString(R.string.title_settings_activity)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        prefs_edit_autocomplete.setOnClickListener {
            val text = EditText(this)
            text.setText(readSharedPrefs(this@SettingsActivity))

            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.dialog_rcon_autocomplete))
                setMessage(getString(R.string.dialog_rcon_autocomplete_message))
                setView(text)
                setPositiveButton(getString(R.string.dialog_update)) { _, _ ->
                    writeSharedPrefs(text.text.toString())
                }
                setNegativeButton(getString(R.string.dialog_delete_cancel)) { _, _ -> }
            }.show()
        }

    }

    private fun writeSharedPrefs(string: String) {
        if (sharedPref == null)
            sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        val value = string.replace("\\s".toRegex(), "")

        sharedPref!!.edit().putString(RCON_AUTOFILL, value).apply()
    }

    fun readSharedPrefs(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)!!
                .getString(RCON_AUTOFILL, defaultAutoFill.joinToString(","))!!
    }

}