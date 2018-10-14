package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.util.Base64
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.service.CountdownService
import java.security.MessageDigest
import java.util.*

class MobiregPreferences private constructor(
        private val pref: SharedPreferences
) {
    companion object {
        fun get(context: Context) = MobiregPreferences(
                context.applicationContext.getSharedPreferences("mobireg", Context.MODE_PRIVATE)!!)

        fun encryptPassword(s: String): String {
            val algorithm = android.util.Base64.decode("TUQ1", Base64.DEFAULT)!!
            val digest = MessageDigest.getInstance(String(algorithm))!!
            digest.update(ByteArray(s.length) { s[it].toByte() })
            val messageDigest = digest.digest()
            val builder = StringBuilder()
            messageDigest.forEach {
                val h = java.lang.Integer.toHexString(it.toInt().and(255))
                if (h.length < 2)
                    builder.append('0')
                builder.append(h)
            }
            return builder.toString()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getString(key: String)
            : String? = pref.getString(key, null)


    fun setRandomizedDeviceId(): Int {
        val id = (Random().nextInt() % 10000 + 50000) * 3
        pref.edit()
                .putString("deviceId", "$id")
                .apply()
        return id
    }

    private fun getDeviceIdOrRandomize(): Int {
        if (pref.contains("deviceId"))
            return pref.getString("deviceId", null)?.toIntOrNull() ?: 0
        return setRandomizedDeviceId()
    }

    fun setLmt(millisOfRequest: Long) {
        pref.edit().putLong("lmt", millisOfRequest).apply()
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        pref.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        pref.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun setUserData(studentId: Int, name: String, surname: String, phone: String, sex: String, loginName: String, host: String, password: String) {
        pref.edit().apply {
            putBoolean("isSignedIn", true)

            putInt("userId", studentId)
            putString("name", name)
            putString("surname", surname)
            putString("phone", phone)
            putString("sex", sex)

            putString("login", loginName)
            putString("host", host)
            putString("pass", password)
        }.apply()
    }

    fun logout(context: Context) {
        LogOutTask(context, pref).exec()
    }

    fun setRefreshFrequency(newFrequency: Int) {
        pref.edit().putString("refreshFrequency", "$newFrequency").apply()
    }

    fun setPassword(notEncryptedNewPassword: String) {
        pref.edit().putString("pass", encryptPassword(notEncryptedNewPassword)).apply()
    }

    fun setLastRefreshTimeToNow() {
        pref.edit().putLong("lastCheck", Calendar.getInstance().timeInMillis).apply()
    }

    fun becomeDeveloper() {
        pref.edit().putBoolean("is_dev", true).apply()
    }

    private class LogOutTask(context: Context, val pref: SharedPreferences)
        : AsyncTask<Unit, Unit, Unit>() {
        @SuppressLint("StaticFieldLeak")
        private val appContext = context.applicationContext!!

        fun exec(): LogOutTask {
            execute()
            return this
        }

        @SuppressLint("ApplySharedPref")
        override fun doInBackground(vararg params: Unit?) {
            AppDatabase.deleteDatabase(appContext)

            CountdownService.stop(appContext)

            pref.edit()
                    .putBoolean("isSignedIn", false)
                    .remove("login")
                    .remove("pass")
                    .remove("host")
                    .remove("userId")
                    .remove("lastEndDate")
                    .remove("startDate")
                    .remove("endDate")
                    .remove("lmt")
                    .remove("name")
                    .remove("surname")
                    .remove("phone")
                    .remove("sex")
                    .remove("lastCheck")
                    .commit()

            TimetableWidgetProvider.requestInstantUpdate(appContext)
        }
    }


    val isSignedIn get() = pref.getBoolean("isSignedIn", false)

    val name get() = getString("name") ?: ""

    val surname get() = getString("surname") ?: ""

    val theme get() = getString("theme") ?: "light"

    val runCountdownService get() = pref.getBoolean("runCountdownService", false)

    val login get() = getString("login")

    val host get() = getString("host")

    val password get() = getString("pass")

    val lmt get() = pref.getLong("lmt", -1L)

    val logEverySync get() = BuildConfig.DEBUG || pref.getBoolean("logEverySync", false)

    val deviceId get() = getDeviceIdOrRandomize()

    val startDate get() = getString("startDate") ?: "2018-01-01"

    val getAllMarkGroups get() = pref.getBoolean("getAllMarkGroups", true)

    val studentId get() = pref.getInt("userId", -1)

    val sex get() = getString("sex") ?: ""

    val lastCheckTime get() = pref.getLong("lastCheck", 0L)

    val refreshFrequency get() = (getString("refreshFrequency") ?: "480").toInt()

    val refreshOnWeekends get() = pref.getBoolean("refreshWeekends", true)

    val notifyWithSound get() = pref.getBoolean("notifyWithSound", true)

    val notifyAboutAttendances get() = pref.getBoolean("notifyAboutAttendances", false)

    val beforeLessonsMinutes: Int
        get() = pref.getString("beforeLessonsMinutes", null)?.toIntOrNull() ?: 45

    val defaultFragment get() = getString("defFrag") ?: MainActivity.FRAGMENT_MARKS

    var lastSelectedTerm: Int
        get() = pref.getInt("lastTerm", 0)
        set(value) = pref.edit().putInt("lastTerm", value).apply()


    val isDeveloper: Boolean = BuildConfig.DEBUG || pref.getBoolean("is_dev", false)

    var seenWelcomeActivity
        get() = pref.getBoolean("seenWA", false)
        set(value) = pref.edit().putBoolean("seenWA", value).apply()
}