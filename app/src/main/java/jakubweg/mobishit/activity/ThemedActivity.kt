package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.MobiregPreferences
import java.util.*

interface ThemedActivity {
    companion object {
        const val THEME_DARK = "dark"
        const val THEME_BLACK = "black"
        const val THEME_AUTO_DARK = "aDark"
        const val THEME_AUTO_BLACK = "aBlack"
        const val THEME_LIGHT = "light"
    }

    fun handleTheme(activity: Activity, theme: String?) {
        if (theme == null)
            return
        activity.setTheme(when (theme) {
            THEME_LIGHT -> return // ignore - default is light
            THEME_DARK -> R.style.AppDarkTheme
            THEME_BLACK -> R.style.AppBlackTheme
            THEME_AUTO_DARK -> if (shouldUseLightTheme()) return else R.style.AppDarkTheme
            THEME_AUTO_BLACK -> if (shouldUseLightTheme()) return else R.style.AppBlackTheme
            else -> return
        })
    }

    private fun shouldUseLightTheme(): Boolean {
        return Calendar.getInstance().run {
            when (get(Calendar.MONTH)) {
                in Calendar.APRIL..Calendar.AUGUST -> get(Calendar.HOUR_OF_DAY) in 6..19
                else -> get(Calendar.HOUR_OF_DAY) in 7..18
            }
        }
    }

    @SuppressLint("Registered")
    open class FragmentActivity : android.support.v4.app.FragmentActivity(), ThemedActivity {
        private lateinit var themedPrefs: MobiregPreferences
        private var lastTheme: String? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            themedPrefs = MobiregPreferences.get(this)
            lastTheme = themedPrefs.theme
            handleTheme(this, lastTheme)
        }

        override fun onStart() {
            super.onStart()
            if (themedPrefs.theme != lastTheme) {
                recreate()
            }
        }
    }

    @SuppressLint("Registered")
    open class AppCompatActivity : android.support.v7.app.AppCompatActivity(), ThemedActivity {
        private lateinit var themedPrefs: MobiregPreferences
        private var lastTheme: String? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            themedPrefs = MobiregPreferences.get(this)
            lastTheme = themedPrefs.theme
            handleTheme(this, lastTheme)
        }

        override fun onStart() {
            super.onStart()
            if (themedPrefs.theme != lastTheme) {
                recreate()
            }
        }
    }
}