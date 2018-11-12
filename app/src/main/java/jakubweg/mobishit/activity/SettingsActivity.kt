package jakubweg.mobishit.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * This class is used only for starting MainActivity with action MainActivity.ACTION_SHOW_PREFERENCES
 * Why haven't I deleted it? Because it's started when notification settings are launched
 */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SHOW_PREFERENCES
        })
        finish()
    }
}
