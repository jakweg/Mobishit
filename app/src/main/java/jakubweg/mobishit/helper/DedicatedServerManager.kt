package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.JsonParser
import org.jsoup.Jsoup

class DedicatedServerManager(
        context: Context
) {
    companion object {
        private val mutex = Any()
        private const val API_PROVIDER_LINK = "https://github.com/JakubekWeg/Mobishit/blob/master/app/release/api_provider.json?raw=true"

        private const val MAX_SYNC_DELAY_MILLIS = 12 * 60 * 60 * 1000L

        private const val KEY_LAST_SYNC_TIME = "last_sync"

        private const val KEY_VERSION_INFO = "version_info"
        private const val KEY_FCM_HANDLER = "fcm_handler"
        private const val KEY_AVERAGES = "averages"
        private const val KEY_TESTS = "tests"
        private const val KEY_CRASH_REPORTS = "crashes"
    }

    private val preferences =
            context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)!!

    @SuppressLint("ApplySharedPref")
    private fun updateServerInfoIfNeeded() {
        val lastSync = preferences.getLong(KEY_LAST_SYNC_TIME, 0L)
        if (lastSync + MAX_SYNC_DELAY_MILLIS > System.currentTimeMillis())
            return

        val body = Jsoup
                .connect(API_PROVIDER_LINK)
                .ignoreContentType(true)
                .execute()
                .body()

        val jo = JsonParser()
                .parse(body)!!
                .asJsonObject!!

        val fcmHandler = jo[KEY_FCM_HANDLER]?.asString!!
        val averages = jo[KEY_AVERAGES]?.asString!!
        val tests = jo[KEY_TESTS]?.asString!!
        val versionInfo = jo[KEY_VERSION_INFO]?.asString!!
        val crashes = jo[KEY_CRASH_REPORTS]?.asString!!

        preferences
                .edit()
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .putString(KEY_VERSION_INFO, versionInfo)
                .putString(KEY_FCM_HANDLER, fcmHandler)
                .putString(KEY_AVERAGES, averages)
                .putString(KEY_TESTS, tests)
                .putString(KEY_CRASH_REPORTS, crashes)
                .commit()
    }

    private fun getAndUpdateIfNeeded(key: String): String? {
        synchronized(mutex) {
            updateServerInfoIfNeeded()
            return preferences.getString(key, null)
        }
    }


    val versionInfoLink get() = getAndUpdateIfNeeded(KEY_VERSION_INFO)!!

    val fcmHandlerLink get() = getAndUpdateIfNeeded(KEY_FCM_HANDLER)!!

    val testsLink get() = getAndUpdateIfNeeded(KEY_TESTS)!!

    val averagesLink get() = getAndUpdateIfNeeded(KEY_AVERAGES)!!

    val crashReportsLink get() = getAndUpdateIfNeeded(KEY_CRASH_REPORTS)!!
}
