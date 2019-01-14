package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.service.AppUpdateWorker
import jakubweg.mobishit.service.FcmServerNotifierWorker
import jakubweg.mobishit.service.UpdateWorker

@Suppress("NOTHING_TO_INLINE")
object SettingsMigrationHelper {
    private const val CURRENT_APP_SETTINGS_VERSION = 11

    @SuppressLint("ApplySharedPref")
    fun onSettingsLoaded(prefs: SharedPreferences?,
                         context: Context) {
        prefs ?: return
        val version = prefs.getInt("version", 0)
        if (version != CURRENT_APP_SETTINGS_VERSION) {
            prefs.edit()
                    .putInt("version", CURRENT_APP_SETTINGS_VERSION)
                    .commit()

            if (version <= 10) { //TODO delete in future

                //this handles crashes
                prefs.edit().putBoolean("hsVm", false).commit()
            } else {
                deleteDatabaseAndRequestNew(prefs, context)
            }
        }
    }

    fun deleteDatabaseAndRequestNew(pref: SharedPreferences,
                                    context: Context) {

        AppDatabase.deleteDatabase(context)
        pref.edit()
                .remove("lastEndDate")
                .remove("startDate")
                .remove("endDate")
                .remove("lastCheck")
                .remove("lmt")
                .remove("hsVm")
                .apply()

        UpdateWorker.requestUpdates(context)
        AppUpdateWorker.requestChecks()
        FcmServerNotifierWorker.requestPeriodicServerNotifications()
    }
}