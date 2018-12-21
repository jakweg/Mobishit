package jakubweg.mobishit.service

import android.content.Context
import android.support.v4.app.NotificationCompat
import android.util.Base64
import androidx.work.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.NotificationHelper
import org.jsoup.Jsoup
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class FcmServerNotifierWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    companion object {
        private const val UNIQUE_WORK_NAME = "fcmServerNotifier"
        private const val NOTIFY_FREQUENCY = 2L
        private val NOTIFY_FREQUENCY_UNIT = TimeUnit.DAYS

        private val networkConstraints
            get() =
                Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

        fun requestPeriodicServerNotifications() {
            val request = PeriodicWorkRequest
                    .Builder(FcmServerNotifierWorker::class.java,
                            NOTIFY_FREQUENCY, NOTIFY_FREQUENCY_UNIT)
                    .setConstraints(networkConstraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1L, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance().apply {
                enqueueUniquePeriodicWork(
                        UNIQUE_WORK_NAME,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        request)
            }
        }
    }

    override fun doWork(): Result {
        return sendNewTokenToServer()
    }


    private fun sendNewTokenToServer(): Result {
        val token = MobiregPreferences.get(applicationContext).firebaseToken

        if (token.isNullOrBlank()) {
            // probably user has no Google Play services :\
            if (BuildConfig.DEBUG)
                postErrorNotification("Token is empty!")
            return Result.failure()
        }

        return try {
            val body = Jsoup
                    .connect(createServerNotifyUrl() ?: return Result.success())!!
                    .ignoreHttpErrors(true)!!
                    .ignoreContentType(true)!!
                    .execute()!!
                    .body()!!

            val jsonObject = JsonParser()
                    .parse(body)
                    .asJsonObject

            val isSuccess = jsonObject["success"]?.asBoolean ?: false
            val result = jsonObject["result"]?.asString ?: "no result provided"

            if (isSuccess) {
                Result.success()
            } else {
                postErrorNotification("Can't register FCM: $result\nGot: $body")
                Result.failure()
            }

        } catch (te: SocketTimeoutException) {
            Result.retry()
        } catch (uhe: UnknownHostException) {
            Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            postErrorNotification(e.localizedMessage)
            Result.failure()
        }
    }


    private fun createServerNotifyUrl(): String? {
        val fcmLink = DedicatedServerManager(applicationContext).fcmHandlerLink ?: return null
        val prefs = MobiregPreferences.get(applicationContext)

        val token = prefs.firebaseToken ?: return null

        val request = JsonObject().apply {
            addProperty("token", token)
            addProperty("logged", prefs.isSignedIn)
            if (prefs.allowedInstantNotifications) {
                addProperty("login", prefs.loginAndHostIfNeeded)
                addProperty("pass", prefs.password)
                addProperty("host", prefs.host)
                addProperty("studentId", prefs.studentId.toString())
            }
            addProperty("version", BuildConfig.VERSION_CODE)
        }.toString()

        return "$fcmLink?token=${URLEncoder.encode(encodeBase64(request), "UTF-8")}"
    }

    private fun encodeBase64(s: String): String {
        return Base64.encodeToString(
                s.toByteArray(), Base64.DEFAULT)!!
    }

    private fun postErrorNotification(message: String) = postErrorNotification("FcmServerNotifierWorker", message)

    private fun postErrorNotification(title: String, message: String) {
        if (!MobiregPreferences.get(applicationContext).isDeveloper)
            return

        NotificationHelper(applicationContext).apply {
            createNotificationChannels()

            val notification = NotificationCompat
                    .Builder(applicationContext, NotificationHelper.CHANNEL_APP_STATE)
                    .setSmallIcon(R.drawable.ic_error)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setStyle(NotificationCompat
                            .BigTextStyle()
                            .bigText(message))

            postNotification(notification)
        }
    }
}