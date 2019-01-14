package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import org.jsoup.Jsoup
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DedicatedServerManager(
        context: Context
) {
    companion object {
        private val mutex = Any()
        private const val API_PROVIDER_LINK = "https://github.com/JakubekWeg/Mobishit/blob/master/app/release/api_provider.json?raw=true"

        private const val MAX_SYNC_DELAY_MILLIS = 2 * 24 * 60 * 60 * 1000L

        private const val KEY_LAST_SYNC_TIME = "last_sync"

        private const val KEY_TERMS_OF_USE = "rules"
        private const val KEY_VERSION_INFO = "version_info"
        private const val KEY_FCM_HANDLER = "fcm_handler"
        private const val KEY_AVERAGES = "averages"
        private const val KEY_TESTS = "tests"
        private const val KEY_CRASH_REPORTS = "crashes"
        private const val KEY_MESSAGES = "messages"

        fun makePostRequest(url: String, postData: ByteArray): String {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 20 * 1000
                requestMethod = "POST"
                doInput = true
                doOutput = true
            }


            val os = connection.outputStream

            BufferedOutputStream(os).use {
                it.write(postData)
            }

            os.close()

            val bis = BufferedInputStream(connection.inputStream)
            val buf = ByteArrayOutputStream()

            val buffer = ByteArray(1024 * 8)
            var read = 0
            while (read >= 0) {
                read = bis.read(buffer)
                if (read == -1)
                    break
                buf.write(buffer, 0, read)
                if (read == 0)
                    Thread.sleep(10L)
            }

            bis.close()
            connection.disconnect()

            return String(buf.toByteArray(), Charsets.UTF_8)
        }
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

        val termsOfUse = jo[KEY_TERMS_OF_USE]?.asString!!
        val fcmHandler = jo[KEY_FCM_HANDLER]?.asString!!
        val averages = jo[KEY_AVERAGES]?.asString!!
        val tests = jo[KEY_TESTS]?.asString!!
        val versionInfo = jo[KEY_VERSION_INFO]?.asString!!
        val crashes = jo[KEY_CRASH_REPORTS]?.asString!!
        val messages = jo[KEY_MESSAGES]?.asString!!

        preferences
                .edit()
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .putString(KEY_TERMS_OF_USE, termsOfUse)
                .putString(KEY_VERSION_INFO, versionInfo)
                .putString(KEY_FCM_HANDLER, fcmHandler)
                .putString(KEY_AVERAGES, averages)
                .putString(KEY_TESTS, tests)
                .putString(KEY_CRASH_REPORTS, crashes)
                .putString(KEY_MESSAGES, messages)
                .commit()
    }

    private fun getAndUpdateIfNeeded(key: String): String? {
        synchronized(mutex) {
            try {
                updateServerInfoIfNeeded()
            } catch (e: Exception) {
                Log.e("DedicatedServerManager", "Can't get information", e)
            }
            return preferences.getString(key, null)
        }
    }

    fun makeExpired() {
        preferences
                .edit()
                .putLong(KEY_LAST_SYNC_TIME, 0L)
                .apply()
    }

    val termsOfUseLink get() = getAndUpdateIfNeeded(KEY_TERMS_OF_USE)

    val versionInfoLink get() = getAndUpdateIfNeeded(KEY_VERSION_INFO)

    val fcmHandlerLink get() = getAndUpdateIfNeeded(KEY_FCM_HANDLER)

    val testsLink get() = getAndUpdateIfNeeded(KEY_TESTS)

    val averagesLink get() = getAndUpdateIfNeeded(KEY_AVERAGES)

    val crashReportsLink get() = getAndUpdateIfNeeded(KEY_CRASH_REPORTS)

    val messagesLink get() = getAndUpdateIfNeeded(KEY_MESSAGES)
}
