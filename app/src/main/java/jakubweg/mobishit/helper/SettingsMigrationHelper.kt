package jakubweg.mobishit.helper

import android.content.Context
import android.content.SharedPreferences
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.service.UpdateWorker

@Suppress("NOTHING_TO_INLINE")
object SettingsMigrationHelper {
    private const val CURRENT_APP_SETTINGS_VERSION = 4

    fun onSettingsLoaded(prefs: SharedPreferences?,
                         context: Context) {
        prefs ?: return
        val version = prefs.getInt("version", 0)
        if (version != CURRENT_APP_SETTINGS_VERSION
                && prefs.contains("version")) {
            prefs.edit().putInt("version", CURRENT_APP_SETTINGS_VERSION).apply()

            deleteDatabaseAndRequestNew(prefs, context)
        }
    }

    private fun deleteDatabaseAndRequestNew(pref: SharedPreferences, context: Context) {
        AppDatabase.deleteDatabase(context)
        pref.edit()
                .remove("lastEndDate")
                .remove("startDate")
                .remove("endDate")
                .remove("lastCheck")
                .remove("lmt")
                .apply()

        if (pref.getBoolean("isSignedIn", false))
            UpdateWorker.requestUpdates(context)
    }
}