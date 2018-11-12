package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.os.Bundle
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.ThemeHelper

@SuppressLint("Registered")
open class FragmentActivity : android.support.v4.app.FragmentActivity() {

    private lateinit var themedPrefs: MobiregPreferences
    private var lastTheme: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themedPrefs = MobiregPreferences.get(this)
        lastTheme = themedPrefs.theme
        ThemeHelper.makeActivityThemed(this, lastTheme)
    }

    override fun onStart() {
        super.onStart()
        if (themedPrefs.theme != lastTheme) {
            recreate()
        }
    }
}