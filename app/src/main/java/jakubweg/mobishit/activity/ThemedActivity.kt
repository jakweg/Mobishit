package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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

    fun openLink(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }

    override fun onStart() {
        super.onStart()
        if (themedPrefs.theme != lastTheme) {
            startActivity(intent)
        }
    }
}