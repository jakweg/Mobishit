@file:Suppress("NOTHING_TO_INLINE")

package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.fragment.AboutVirtualMarksFragment
import jakubweg.mobishit.helper.ThemeHelper.THEME_DEFAULT
import jakubweg.mobishit.service.CountdownService
import jakubweg.mobishit.service.FcmServerNotifierWorker
import java.security.MessageDigest
import java.util.*

class MobiregPreferences private constructor(
        context: Context,
        val prefs: SharedPreferences
) {
    companion object {
        const val ATTENDANCE_NOTIFICATION_CHANGE = 1 shl 0
        const val ATTENDANCE_NOTIFICATION_ABSENT = 1 shl 1
        const val ATTENDANCE_NOTIFICATION_PRESENT = 1 shl 2
        private var INSTANCE: MobiregPreferences? = null

        fun get(context: Context?): MobiregPreferences {
            context ?: return INSTANCE ?: throw NullPointerException()
            return INSTANCE ?: MobiregPreferences(context.applicationContext,
                    context.applicationContext.getSharedPreferences("mobireg", Context.MODE_PRIVATE)!!).also {
                INSTANCE = it
            }
        }

        fun encryptPassword(s: String?): String {
            if (s == null) throw NullPointerException()
            val chars = "0123456789abcdef"
            val bytes = MessageDigest
                    .getInstance("MD5")
                    .digest(s.toByteArray())
            val result = StringBuilder(bytes.size * 2)

            bytes.forEach {
                val i = it.toInt()
                result.append(chars[i shr 4 and 0x0f])
                result.append(chars[i and 0x0f])
            }

            return result.toString()
        }
    }

    init {
        SettingsMigrationHelper.onSettingsLoaded(this, context)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getString(key: String)
            : String? = prefs.getString(key, null)


    private fun setRandomizedDeviceId(): Int {
        val id = (Random().nextInt() % 10000 + 50000) * 3
        prefs.edit()
                .putString("deviceId", "$id")
                .apply()
        return id
    }

    private fun getDeviceIdOrRandomize(): Int {
        if (prefs.contains("deviceId"))
            return prefs.getString("deviceId", null)?.toIntOrNull() ?: 0
        return setRandomizedDeviceId()
    }

    fun setLmt(millisOfRequest: Long) {
        prefs.edit().putLong("lmt", millisOfRequest).apply()
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setUserData(studentId: Int, name: String, surname: String, phone: String, sex: String, loginName: String, host: String, hasHostInLogin: Boolean, password: String) {
        prefs.edit().apply {
            putBoolean("isSignedIn", true)

            putInt("userId", studentId)
            putString("name", name)
            putString("surname", surname)
            putString("phone", phone)
            putString("sex", sex)

            putString("login", loginName)
            putString("host", host)
            putBoolean("hasHostInLogin", hasHostInLogin)
            putString("pass", password)
        }.apply()
    }

    fun logout(context: Context) {
        LogOutTask(context, prefs).exec()
    }

    fun setRefreshFrequency(newFrequency: Int) {
        prefs.edit().putString("refreshFrequency", "$newFrequency").apply()
    }

    fun setPassword(notEncryptedNewPassword: String?) {
        notEncryptedNewPassword ?: throw NullPointerException()
        prefs.edit().putString("pass", encryptPassword(notEncryptedNewPassword)).apply()
    }

    fun setLastRefreshTimeToNow() {
        prefs.edit().putLong("lastCheck", Calendar.getInstance().timeInMillis).apply()
    }

    fun setLastFcmAction(action: String?) {
        prefs.edit().putString("lastFcmAct", action)
                .putLong("lastFcmTime", System.currentTimeMillis())
                .apply()
    }

    fun setAppUpdateInfo(newCode: Int, newName: String, urlDoDownload: String, whatIsNew: String?) {
        prefs.edit().putInt("aUnewCode", newCode)
                .putString("aUnewName", newName)
                .putString("aUurl", urlDoDownload)
                .putString("aUnews", whatIsNew)
                .apply()
    }

    fun getAppUpdateLink(): String? {
        if (prefs.getInt("aUnewCode", BuildConfig.VERSION_CODE) <= BuildConfig.VERSION_CODE)
            return null

        return getString("aUurl") ?: "https://github.com/JakubekWeg/Mobishit"
    }

    fun markLastUsedVersionCurrent() {
        lastUsedVersion = BuildConfig.VERSION_CODE
    }

    private class LogOutTask(context: Context, private val pref: SharedPreferences)
        : AsyncTask<Unit, Unit, Unit>() {
        @SuppressLint("StaticFieldLeak")
        private val appContext = context.applicationContext!!

        fun exec(): LogOutTask {
            execute()
            return this
        }

        @SuppressLint("ApplySharedPref")
        override fun doInBackground(vararg params: Unit?) {
            CountdownService.stop(appContext)

            pref.edit()
                    .putBoolean("isSignedIn", false)
                    .remove("login")
                    .remove("pass")
                    .remove("host")
                    .remove("hasHostInLogin")
                    .remove("userId")
                    .remove("lastEndDate")
                    .remove("startDate")
                    .remove("endDate")
                    .remove("lastCheck")
                    .remove("lmt")
                    .remove("name")
                    .remove("surname")
                    .remove("phone")
                    .remove("sex")
                    .remove("lastTestRefresh")
                    .remove("readyAverage")
                    .remove("lmsg")
                    .commit()

            AppDatabase.deleteDatabase(appContext)

            TimetableWidgetProvider.requestInstantUpdate(appContext)

            FcmServerNotifierWorker.requestPeriodicServerNotifications()
        }
    }


    val isSignedIn get() = prefs.getBoolean("isSignedIn", false)

    val name get() = getString("name") ?: ""

    val surname get() = getString("surname") ?: ""

    val theme get() = getString("theme") ?: THEME_DEFAULT

    val runCountdownService get() = prefs.getBoolean("runCountdownService", false)

    val login get() = getString("login")

    val host get() = getString("host")

    val password get() = getString("pass")

    private val hasHostInLogin get() = prefs.getBoolean("hasHostInLogin", true)

    val loginAndHostIfNeeded get() = if (hasHostInLogin) "$login.$host" else "$login"

    val lmt get() = prefs.getLong("lmt", -1L)

    val deviceId get() = getDeviceIdOrRandomize()

    val startDate get() = getString("startDate") ?: "2018-01-01"

    val getAllMarkGroups get() = prefs.getBoolean("getAllMarkGroups", true)

    val studentId get() = prefs.getInt("userId", -1)

    val sex get() = getString("sex") ?: ""

    val lastCheckTime get() = prefs.getLong("lastCheck", 0L)

    val refreshFrequency get() = (getString("refreshFrequency") ?: "480").toInt()

    val notifyWithSound get() = prefs.getBoolean("notifyWithSound", true)

    val beforeLessonsMinutes: Int
        get() = prefs.getString("beforeLessonsMinutes", null)?.toIntOrNull() ?: 45

    val defaultFragment get() = getString("defFrag") ?: MainActivity.FRAGMENT_MARKS

    var lastSelectedTerm: Int
        get() = prefs.getInt("lastTerm", 0)
        set(value) = prefs.edit().putInt("lastTerm", value).apply()

    var downloadedComparisonsTermId: Int
        get() = prefs.getInt("dcti", 0)
        set(value) = prefs.edit().putInt("dcti", value).apply()

    var seenWelcomeActivity
        get() = prefs.getBoolean("seenWA", false)
        set(value) = prefs.edit().putBoolean("seenWA", value).apply()

    var nextAllowedCountdownServiceStart
        get() = prefs.getLong("nAllowedCDSstart", 0L)
        set(value) = prefs.edit().putLong("nAllowedCDSstart", value).apply()

    var firebaseToken: String?
        get() = getString("fcmToken")
        set(value) = prefs.edit().putString("fcmToken", value).apply()

    var lastTokenUploadMillis: Long
        get() = prefs.getLong("ltum", 0)
        set(value) = prefs.edit().putLong("ltum", value).apply()

    var allowedInstantNotifications
        get() = prefs.getBoolean("allowIN", false)
        set(value) {
            decidedAboutFcm = true
            prefs.edit().putBoolean("allowIN", value).apply()
            FcmServerNotifierWorker.requestPeriodicServerNotifications()
        }

    var decidedAboutFcm
        get() = prefs.getBoolean("decidedFcm", false)
        set(value) = prefs.edit().putBoolean("decidedFcm", value).apply()

    val lastFcmAction
        get() = getString("lastFcmAct")


    var lastTestRefreshTime
        get() = prefs.getLong("lastTestRefresh", 0L)
        set(value) = prefs.edit().putLong("lastTestRefresh", value).apply()

    var lastComparisonsRefreshTime
        get() = prefs.getLong("lastComparisonsRefresh", 0L)
        set(value) = prefs.edit().putLong("lastComparisonsRefresh", value).apply()

    var groupMarksByParent
        get() = prefs.getBoolean("groupMarks", true)
        set(value) = prefs.edit().putBoolean("groupMarks", value).apply()

    var markSortingOrder
        get() = prefs.getInt("mSortOrder", AverageCalculator.ORDER_DEFAULT)
        set(value) = prefs.edit().putInt("mSortOrder", value).apply()

    var hasReadyAverageCache
        get() = prefs.getBoolean("readyAverage", false)
        set(value) = prefs.edit().putBoolean("readyAverage", value).apply()

    var lastUsedVersion
        get() = prefs.getInt("lUV", 0)
        set(value) = prefs.edit().putInt("lUV", value).apply()

    var notifyWhenMainActivityIsInForeground
        get() = prefs.getBoolean("notifyWhenFG", true)
        set(value) = prefs.edit().putBoolean("notifyWhenFG", value).apply()

    var ignoreCrashes
        get() = prefs.getBoolean("iC", false)
        set(value) = prefs.edit().putBoolean("iC", value).apply()

    var lastCrashTime
        get() = prefs.getLong("lct", 0L)
        @SuppressLint("ApplySharedPref")
        set(value) {
            prefs.edit().putLong("lct", value).commit()
        }

    var hasReadyWidgetCache
        get() = prefs.getBoolean("hrWidc", false)
        set(value) = prefs.edit().putBoolean("hrWidc", value).apply()

    var showLastMarks
        get() = prefs.getBoolean("sLM", true)
        set(value) = prefs.edit().putBoolean("sLM", value).apply()

    var hasReadyLastMarksCache
        get() = prefs.getBoolean("sLMc", true)
        set(value) = prefs.edit().putBoolean("sLMc", value).apply()

    var seenAboutAttendanceFragment
        get() = prefs.getBoolean("sAAf", false)
        set(value) = prefs.edit().putBoolean("sAAf", value).apply()

    val showLessonNumberOnTimetable get() = showingLessonsNumberPolicy[0] == '1'
    val showLessonNumberOnWidget get() = showingLessonsNumberPolicy[1] == '1'
    private val showingLessonsNumberPolicy get() = getString("sn") ?: "11"


    val savedVirtualMarksState get() = prefs.getInt("svms", AboutVirtualMarksFragment.STATE_NO_MARKS_SAVED)

    val savedMarkScaleGroupId get() = prefs.getInt("smsg", -1)

    inline fun markHavingPointsMarks() = setSavedMarksState(AboutVirtualMarksFragment.STATE_HAVING_POINTS_MARKS, -1)
    inline fun markHavingNoSavedMarks() = setSavedMarksState(AboutVirtualMarksFragment.STATE_NO_MARKS_SAVED, 0)

    @SuppressLint("ApplySharedPref")
    fun setSavedMarksState(state: Int, markScaleGroupsId: Int, clearSaved: Boolean) {
        prefs.edit()
                .putInt("svms", state)
                .putInt("smsg", markScaleGroupsId)
                .putBoolean("cvm", clearSaved)
                .commit()
    }

    val shouldClearVirtualMarks
        get(): Boolean {
            val v = prefs.getBoolean("cvm", false)
            if (v)
                prefs.edit().remove("cvm").apply()
            return v
        }

    fun setSavedMarksState(state: Int, markScaleGroupsId: Int) = setSavedMarksState(state, markScaleGroupsId, false)

    var attendanceNotificationPolicy
        get() = prefs.getInt("apo", ATTENDANCE_NOTIFICATION_ABSENT or ATTENDANCE_NOTIFICATION_CHANGE)
        set(value) = prefs.edit().putInt("apo", value).apply()

}