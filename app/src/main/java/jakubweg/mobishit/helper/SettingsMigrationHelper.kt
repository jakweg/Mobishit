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
    private const val CURRENT_APP_SETTINGS_VERSION = 12

    @SuppressLint("ApplySharedPref")
    fun onSettingsLoaded(prefs: MobiregPreferences?,
                         context: Context) {
        prefs ?: return
        val version = prefs.prefs.getInt("version", 0)
        if (version != CURRENT_APP_SETTINGS_VERSION) {
            if (prefs.prefs.contains("notifyAboutAttendances")) {
                if (prefs.prefs.getBoolean("notifyAboutAttendances", false))
                    prefs.attendanceNotificationPolicy = prefs.attendanceNotificationPolicy or MobiregPreferences.ATTENDANCE_NOTIFICATION_PRESENT

                prefs.prefs.edit().remove("notifyAboutAttendances").commit()
            }

            prefs.prefs.edit()
                    .putInt("version", CURRENT_APP_SETTINGS_VERSION)
                    .commit()

            if (version < 11)
                deleteDatabaseAndRequestNew(prefs.prefs, context)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun deleteDatabaseAndRequestNew(pref: SharedPreferences,
                                    context: Context) {

        pref.edit()
                .remove("lastEndDate")
                .remove("startDate")
                .remove("endDate")
                .remove("lastCheck")
                .remove("lmt")
                .remove("hsVm")
                .commit()
        AppDatabase.deleteDatabase(context)

        UpdateWorker.requestUpdates(context)
        AppUpdateWorker.requestChecks()
        FcmServerNotifierWorker.requestPeriodicServerNotifications()
    }
}