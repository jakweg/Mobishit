package jakubweg.mobishit.helper

import android.content.Context
import android.content.SharedPreferences
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.service.UpdateWorker

@Suppress("NOTHING_TO_INLINE")
object SettingsMigrationHelper {
    private const val CURRENT_APP_SETTINGS_VERSION = 3

    fun onSettingsLoaded(prefs: SharedPreferences?,
                         context: Context) {
        prefs ?: return
        val version = prefs.getInt("version", 0)
        if (version != CURRENT_APP_SETTINGS_VERSION
                && prefs.contains("version")) {
            prefs.edit().putInt("version", CURRENT_APP_SETTINGS_VERSION).apply()
            when (version) {
                0 -> update1to2(prefs, context)
                2 -> update2to3(prefs, context)
            }
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
        UpdateWorker.requestUpdates(context)
    }

    private inline fun update1to2(pref: SharedPreferences, context: Context) {
        deleteDatabaseAndRequestNew(pref, context)
    }

    private inline fun update2to3(pref: SharedPreferences, context: Context) {
        deleteDatabaseAndRequestNew(pref, context)
    }
}