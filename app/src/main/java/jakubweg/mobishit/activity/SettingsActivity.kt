package jakubweg.mobishit.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.MobiregPreferences

/**
 * This class is used only for starting MainActivity with action MainActivity.ACTION_SHOW_PREFERENCES
 * Why haven't I deleted it? Because it's started when notification settings are launched
 * The second thing it does – it shows up the crash dialog
 */
class SettingsActivity : Activity() {
    companion object {
        const val ACTION_SHOW_CRASH_DIALOG = "sc"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == ACTION_SHOW_CRASH_DIALOG) {
            setTheme(R.style.AppBlackTheme)
            AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("Przepraszam, to nie powinno się stać")
                    .setMessage("Aplikacja Mobishit napotkała błąd krytyczny i nie może kontynuować pracy\n" +
                            "Możesz ją otworzyć ponownie lub zatrzymać jej działanie.")
                    .setPositiveButton("Otwórz ponownie") { _, _ ->
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .setNegativeButton("Zamknij") { _, _ ->
                        finish()
                    }
                    .setNeutralButton("Nie pokazuj tego więcej") { _, _ ->
                        MobiregPreferences.get(this).ignoreCrashes
                        finish()
                    }
                    .show()
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_SHOW_PREFERENCES
            })
            finish()
        }
    }
}
