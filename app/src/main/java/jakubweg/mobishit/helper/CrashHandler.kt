package jakubweg.mobishit.helper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler {
    fun getCrashesFolder(c: Context) = File(c.filesDir, "crashes")

    fun onNewCrash(context: Context,
                   thread: Thread,
                   exception: Throwable) {
        val helper = NotificationHelper(context)
        helper.createNotificationChannels()

        val writer = StringWriter()
        writer.append("Błąd krytyczny Mobishit - ")
        writer.append(exception.localizedMessage)
        writer.append('\n')
        exception.printStackTrace(PrintWriter(writer))

        val message = "Mobishit napotkał błąd krytyczny, którego nie jest w stanie obsłużyć. Kluczowe informacje zostały zapisane na urządzeniu. Proszę skontaktuj się z programistą w celu naprawy błędu. (Kliknij aby otworzyć czat)"

        val intent = try {
            PendingIntent.getActivity(context, -1,
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/jakweg")), PendingIntent.FLAG_UPDATE_CURRENT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val builder = NotificationCompat.Builder(
                context, NotificationHelper.CHANNEL_APP_STATE)
                .setContentTitle("Wystąpił błąd krytyczny w Mobishit")
                .setContentInfo(message)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setSmallIcon(R.drawable.ic_error)
                .setContentIntent(intent)!!

        helper.postNotification(builder)

        var file = getCrashesFolder(context)
        if (!file.exists())
            file.mkdirs()

        file = File(file, DateHelper.millisToStringTime(System.currentTimeMillis())
                .replace(":".toRegex(), "_"))

        file.bufferedWriter().use {
            it.write(BuildConfig.APPLICATION_ID)
            it.append('\n')
            it.write(BuildConfig.VERSION_NAME)
            it.append('\n')
            it.write(BuildConfig.VERSION_CODE.toString())
            it.append('\n')
            it.write(Build.VERSION.SDK_INT.toString())
            it.append('\n')
            it.write(Build.PRODUCT)
            it.append('\n')
            it.write(Build.DEVICE)
            it.append('\n')

            it.write(file.name)
            it.append('\n')
            it.write(thread.name)
            it.append('\n')
            it.write(thread.priority.toString())
            it.append('\n')
            it.write(writer.toString())
            it.append('\n')
            MobiregPreferences.get(context).apply {
                it.write(firebaseToken ?: "no_token")
            }

        }

        System.exit(0)
    }
}