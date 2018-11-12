package jakubweg.mobishit.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.app.NotificationCompat
import androidx.work.*
import com.google.gson.JsonParser
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.NotificationHelper
import org.jsoup.Jsoup
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


class AppUpdateWorker(context: Context, workerParameters: WorkerParameters)
    : Worker(context, workerParameters) {

    companion object {
        private const val UNIQUE_WORK_NAME = "appUpdateChecking"
        private const val UPDATE_CHECKING_INTERVAL_DAYS = 1L

        fun requestChecks() {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request = PeriodicWorkRequest.Builder(AppUpdateWorker::class.java,
                    UPDATE_CHECKING_INTERVAL_DAYS, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance()
                    .enqueueUniquePeriodicWork(
                            UNIQUE_WORK_NAME,
                            ExistingPeriodicWorkPolicy.KEEP,
                            request)
        }
    }

    override fun doWork(): Result {
        return try {
            val body = Jsoup
                    .connect(DedicatedServerManager(applicationContext).versionInfoLink)
                    .ignoreContentType(true)
                    .execute().body()

            val obj = JsonParser().parse(body).asJsonObject
            val newCode = obj.get("VERSION_CODE")!!.asInt
            val newName = obj.get("VERSION_NAME")!!.asString!!
            val urlDoDownload = obj.get("URL")!!.asString!!
            val whatIsNew = obj.get("WHATS_NEW")?.asString?.replace("\\n", "\n")

            if (newCode > BuildConfig.VERSION_CODE)
                postNotificationAboutNewVersion(newName, urlDoDownload, whatIsNew)

            Result.SUCCESS
        } catch (ste: SocketTimeoutException) {
            Result.RETRY
        } catch (ie: IOException) {
            ie.printStackTrace()
            Result.FAILURE
        } catch (e: Exception) {
            e.printStackTrace()
            Result.FAILURE
        }
    }

    private fun postNotificationAboutNewVersion(newName: String, uri: String, whatsNew: String?) {
        NotificationHelper(applicationContext).apply {
            createNotificationChannels()

            val notificationIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

            val contentIntent = PendingIntent.getActivity(
                    applicationContext, 0,
                    notificationIntent, 0)

            val notification = NotificationCompat.Builder(applicationContext,
                    NotificationHelper.CHANNEL_APP_STATE)
                    .setSmallIcon(R.drawable.school)
                    .setLargeIcon(BitmapFactory.decodeResource(
                            applicationContext.resources, R.mipmap.ic_launcher_round))
                    .setContentTitle("Nowa wersja aplikacji Mobishit")
                    .setSubText("Kliknij aby pobrać wersję $newName")
                    .setContentText(whatsNew)
                    .setStyle(if (whatsNew == null) null
                    else NotificationCompat.BigTextStyle()
                            .bigText(whatsNew))
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)

            postNotification(notification)
        }
    }
}