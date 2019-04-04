package com.sourceservermanager

import android.os.Bundle
import android.preference.PreferenceActivity

/**
 * Created by Matthew on 2/22/2016.
 */
class SettingsActivity : PreferenceActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }

}
