package jakubweg.mobishit.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spanned
import android.util.Log
import androidx.work.*
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.*
import jakubweg.mobishit.db.AppDatabase.Companion.notifyUpdated
import jakubweg.mobishit.helper.*
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min


class UpdateWorker(context: Context, workerParameters: WorkerParameters)
    : Worker(context, workerParameters) {

    companion object {
        const val UNIQUE_WORK_NAME = "updateMobireg"
        fun requestUpdates(context: Context) {
            val frequency = MobiregPreferences.get(context).run {
                if (!isSignedIn)
                    return
                return@run refreshFrequency.toLong()
            }

            if (frequency == 0L) {
                WorkManager.getInstance().cancelUniqueWork(UNIQUE_WORK_NAME)
            } else {
                val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                val request = PeriodicWorkRequest.Builder(UpdateWorker::class.java,
                        frequency, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()

                WorkManager.getInstance()
                        .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME,
                                ExistingPeriodicWorkPolicy.REPLACE,
                                request)
            }
        }
    }

    override fun doWork(): Result {
        val notificationHelper = NotificationHelper(applicationContext)
        val prefs = MobiregPreferences.get(applicationContext)
        return try {
            if (!prefs.isSignedIn) {
                WorkManager.getInstance()
                        .cancelUniqueWork(UNIQUE_WORK_NAME)
                return Result.SUCCESS
            }

            if (prefs.lastCheckTime + 15 * 1000L > System.currentTimeMillis())
                return Result.SUCCESS

            if (!prefs.refreshOnWeekends)
                Calendar.getInstance().apply {
                    if (!when (this[Calendar.DAY_OF_WEEK]) {
                                Calendar.FRIDAY -> this[Calendar.HOUR_OF_DAY] < 16
                                Calendar.SATURDAY -> true
                                Calendar.SUNDAY -> this[Calendar.HOUR_OF_DAY] > 16
                                else -> false
                            }) {
                        //we've got weekends and we don't care about school, so we get drunk and PARTY!!
                        return Result.SUCCESS
                    }
                }

            val updateHelper = UpdateHelper(applicationContext)

            updateHelper.doUpdate()


            if (updateHelper.isFirstTime
                    || updateHelper.newMarks.isNotEmpty()
                    || updateHelper.deletedMarks.isNotEmpty())
                prefs.hasReadyAverageCache = false


            val db = AppDatabase.getAppDatabase(applicationContext)

            if (!MainActivity.isMainActivityInForeground
                    || prefs.notifyWhenMainActivityIsInForeground) {

                notificationHelper.createNotificationChannels()
                if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                    makeNotificationForNewMarks(notificationHelper, db.markDao, prefs, updateHelper.newMarks)
                    makeNotificationsForDeletedMarks(notificationHelper, prefs, updateHelper.deletedMarks)
                    makeNotificationsForNewMessages(notificationHelper, db.messageDao, prefs, updateHelper.newMessages)
                    makeNotificationsForAttendances(notificationHelper, db.eventDao, prefs, updateHelper.newAttendances)
                    makeNotificationsForEvents(notificationHelper, db.eventDao, prefs, updateHelper.newEvents)
                }
            }


            notifyUpdated(applicationContext, updateHelper.isAnythingNew)

            TimetableWidgetProvider.requestInstantUpdate(applicationContext)

            Result.SUCCESS
        } catch (ste: SocketTimeoutException) {
            outputData = Data.Builder()
                    .putBoolean("success", false)
                    .build()
            Log.e("UpdateWorker", "Got SocketTimeoutException, should retry in 1/2 minute")
            Result.RETRY
        } catch (ipe: UpdateHelper.InvalidPasswordException) {
            postWrongPasswordNotification(notificationHelper, prefs)

            Result.FAILURE
        } catch (e: Exception) {
            e.printStackTrace()

            notificationHelper.postNotification(
                    makeErrorNotification(getKotlinExceptionMessage(e)))

            outputData = Data.Builder()
                    .putBoolean("success", false)
                    .build()
            Result.FAILURE
        } finally {
            System.gc()
        }
    }


    private fun makeNotificationForNewMarks(notificationHelper: NotificationHelper, dao: MarkDao, prefs: MobiregPreferences, list: MutableList<MarkData>) {
        if (list.isEmpty() || notificationHelper.isChannelMuted(NotificationHelper.CHANNEL_MARKS))
            return

        val ids = notificationHelper.getNotificationIds(list.size)
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_MARKS)
                .setSmallIcon(R.drawable.star)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)!!
                .setDefaultsIf(prefs.notifyWithSound)


        val textPrefix = when (prefs.sex) {
            "M" -> "Otrzymałeś"
            "K" -> "Otrzymałaś"
            else -> "Otrzymano"
        }

        val contentIntent = Intent(applicationContext, MainActivity::class.java)
        contentIntent.action = MainActivity.ACTION_SHOW_MARK

        dao.getMarkShortInfo(List(list.size) { list[it].id }).forEachIndexed { index, info ->
            val text = when {
                info.abbreviation != null -> "$textPrefix ${info.abbreviation} za ${info.description}"
                info.markPointsValue >= 0f -> "$textPrefix ${info.markPointsValue} za ${info.description}"
                else -> null
            }
            if (text != null) {
                contentIntent.putExtra("id", info.id)
                notification.setContentTitle("Nowa ocena z ${info.subjectName}")
                        .setContentText(text)
                        .setContentIntent(PendingIntent.getActivity(applicationContext, ids[index], contentIntent, 0))
                notificationHelper.postNotification(ids[index], notification)
            }
        }
    }

    private fun makeNotificationsForDeletedMarks(notificationHelper: NotificationHelper, prefs: MobiregPreferences, list: MutableList<MarkDao.DeletedMarkData>) {
        if (list.isEmpty() || notificationHelper.isChannelMuted(NotificationHelper.CHANNEL_MARKS))
            return

        val ids = notificationHelper.getNotificationIds(list.size)
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_MARKS)
                .setSmallIcon(R.drawable.ic_remove)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)!!
                .setDefaultsIf(prefs.notifyWithSound)

        list.forEachIndexed { index, it ->
            notification.setContentTitle("Usunięto ocenę z ${it.subjectName}")
            if (it.description != null) {
                if (it.abbreviation != null)
                    notification.setContentText("Pozbyto się ${it.abbreviation} za ${it.description}")
                else
                    notification.setContentText("Pozbyto sie oceny za ${it.description}")
            } else {
                notification.setContentText("Ktoś usunął ci ocenę")
            }

            notificationHelper.postNotification(ids[index], notification)
        }
    }

    private fun makeNotificationsForNewMessages(notificationHelper: NotificationHelper, dao: MessageDao, prefs: MobiregPreferences, messages: MutableList<MessageData>) {
        val list = messages.filterNot { it.readTime > 0L }
        if (list.isEmpty() || notificationHelper.isChannelMuted(NotificationHelper.CHANNEL_MESSAGES))
            return

        val ids = notificationHelper.getNotificationIds(list.size)
        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.nav_messages)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)!!
                .setDefaultsIf(prefs.notifyWithSound)

        list.forEachIndexed { index, messageData ->
            val contentIntent = Intent(applicationContext, MainActivity::class.java)
            contentIntent.action = MainActivity.ACTION_SHOW_MESSAGE
            contentIntent.putExtra("id", messageData.id)

            val title = "Wiadomość od ${dao.getTeacherFullName(messageData.senderId)
                    ?: "Nieznanego"}"

            notification.setContentTitle(title)
                    .setContentText(messageData.content)
                    .setSubText(messageData.title)
                    .setWhen(messageData.sendTime)
                    .setStyle(NotificationCompat.BigTextStyle(notification)
                            .setBigContentTitle(title)
                            .bigText(messageData.content)
                            .setSummaryText(messageData.title))
                    .setContentIntent(
                            PendingIntent.getActivity(applicationContext, ids[index], contentIntent, 0)!!)!!
            notificationHelper.postNotification(ids[index], notification)
        }
    }

    private fun makeNotificationsForAttendances(notificationHelper: NotificationHelper, dao: EventDao, prefs: MobiregPreferences, list: MutableList<AttendanceData>) {
        if (list.isEmpty() || notificationHelper.isChannelMuted(NotificationHelper.CHANNEL_ATTENDANCES))
            return

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ATTENDANCES)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)!!
                .setDefaultsIf(prefs.notifyWithSound)

        val canNotifyAboutAttendance = prefs.notifyAboutAttendances

        list.map { it.id }.split(100).forEach { eventIds ->
            dao.getAttendanceInfoByIds(eventIds, canNotifyAboutAttendance).apply {
                val notificationIds = notificationHelper.getNotificationIds(size)
                forEachIndexed { index, info ->
                    notification.setSmallIcon(
                            if (info.isAbsent) R.drawable.event_busy else R.drawable.event_available)
                    //notification.color = info.color

                    notification.setContentTitle(
                            "Wpisano ${info.attendanceName} na ${info.subjectName?.takeUnless { it.isBlank() }
                                    ?: "wydarzeniu"}")

                    val formattedDate = DateHelper.millisToStringDate(info.date)

                    notification.setContentText(if (info.number != null)
                        "Lekcja ${info.number} • $formattedDate • ${info.startTime}"
                    else
                        "Dzień $formattedDate • godzina ${info.startTime}")

                    notificationHelper.postNotification(notificationIds[index], notification)
                }
            }
        }
    }

    private fun makeNotificationsForEvents(notificationHelper: NotificationHelper, dao: EventDao, prefs: MobiregPreferences, list: MutableList<EventData>) {
        if (list.isEmpty() || notificationHelper.isChannelMuted(NotificationHelper.CHANNEL_SUBSTITUTIONS))
            return

        val contentIntent = PendingIntent.getActivity(applicationContext, 1,
                Intent(applicationContext, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_SHOW_TIMETABLE
                }, 0)

        val notification = NotificationCompat.Builder(
                applicationContext, NotificationHelper.CHANNEL_SUBSTITUTIONS)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSmallIcon(R.drawable.event_note)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)!!
                .setDefaultsIf(prefs.notifyWithSound)

        val (replacedEvents, otherLessons) = Pair(mutableListOf<EventData>(),
                mutableListOf<EventData>()).apply {
            list.forEach {
                (if (it.substitution == EventDao.SUBSTITUTION_OLD_LESSON)
                    first else second).add(it)
            }
        }

        val haveEnding = when (prefs.sex) {
            "M" -> ""
            "K" -> "a"
            else -> "(a)"
        }

        val ids = notificationHelper.getNotificationIds(otherLessons.size)

        otherLessons.forEachIndexed { index, event ->
            event.apply {
                notification.setStyle(null)

                val subjectName = dao.getSubjectNameByEventType(eventTypeId)
                val dayName = getWeekDayName(date)
                if (status == EventDao.STATUS_CANCELED) {

                    val formattedDate = DateHelper.millisToStringDate(date)

                    notification.setContentTitle("Odwołano <b>$subjectName</b> w $dayName!".fromHtml())
                    notification.setContentText(if (number != null)
                        "Odwołano $number lekcję w dniu $formattedDate"
                    else
                        "Odwołano wydarzenie w dniu $formattedDate")

                } else if (substitution == EventDao.SUBSTITUTION_NEW_LESSON) {
                    val teacherName = dao.getTeacherFullNameByEventType(eventTypeId)
                            ?: "nieznajomym"
                    val replaced = replacedEvents.find { it.number == number && it.date == date }

                    if (replaced == null) {
                        /*
                        Tutaj wyświetlane było proste powiadomienie o zastępstwie, ale mobireg wysyła event updates nawet jak nic się nie zmieniło
                        Z tego powodu powiadomienie było pokazywane kilka razy, więc nie pokażemy go wcale

                        // Nie znaleziono lekcji która została zastąpiona, będzie trzeba improwizować
                        notification.setContentTitle("Nowe zastępstwo")
                        notification.setContentText("Masz <b>$subjectName</b> z <b>$teacherName</b> w $dayName".fromHtml())
                        */
                        return@forEachIndexed
                    } else {
                        val previousSubject = dao.getSubjectNameByEventType(replaced.eventTypeId)
                        val previousTeacher = dao.getTeacherFullNameByEventType(replaced.eventTypeId)

                        val title = "Nowe zastępstwo w $dayName"
                        val shortContent = "Masz <b>$subjectName</b> z <b>$teacherName</b>".fromHtml()

                        val bigContent = ("W <b>$dayName</b> na lekcji <b>$number</b> będziesz miał$haveEnding " +
                                "<b>$subjectName</b> z <b>$teacherName</b> zamiast " +
                                "<b>$previousSubject</b> z <b>$previousTeacher</b>").fromHtml()

                        notification.setContentTitle(title)
                        notification.setContentText(shortContent)
                        notification.setStyle(NotificationCompat.BigTextStyle()
                                .setBigContentTitle(title)
                                .bigText(bigContent))
                    }
                } else {
                    // To nie powinno się wydarzyć :\
                    return@forEachIndexed
                }

                notificationHelper.postNotification(ids[index], notification)
            }
        }
    }


    private fun <T> List<T>.split(count: Int): List<List<T>> {
        require(count > 0)
        if (size <= count)
            return listOf(this)

        var remain = size
        val iterator = iterator()

        return List(size / count + 1) { _ ->
            List(min(remain, count)) { _ ->
                remain--
                iterator.next()
            }
        }
    }

    private fun getWeekDayName(time: Long): String {
        val millisInDay = 24 * 60 * 60 * 1000

        val now = Calendar.getInstance().timeInMillis / millisInDay * millisInDay

        val weeksDifference = ((time - now) / (7 * millisInDay)).toInt()

        val dayOfWeek = Calendar.getInstance()
                .apply { timeInMillis = time }
                .get(Calendar.DAY_OF_WEEK)

        return when (weeksDifference) {
            0 -> getWeekDayName(dayOfWeek)
            1 -> getNextWeekDayName(dayOfWeek)
            else -> "${getWeekDayName(dayOfWeek)} ${DateHelper.millisToStringDate(time)}"
        }
    }

    private fun getNextWeekDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "przyszły poniedziałek"
            Calendar.TUESDAY -> "przyszły wtorek"
            Calendar.WEDNESDAY -> "przyszłą środę"
            Calendar.THURSDAY -> "przyszły czwartek"
            Calendar.FRIDAY -> "przyszły piątek"
            Calendar.SATURDAY -> "przyszłą sobotę"
            Calendar.SUNDAY -> "przyszłą niedzielę"
            else -> throw IllegalArgumentException()
        }
    }

    private fun getWeekDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "poniedziałek"
            Calendar.TUESDAY -> "ten wtorek" //because w wtorek is wrong, it must be we wtorek
            Calendar.WEDNESDAY -> "środę"
            Calendar.THURSDAY -> "czwartek"
            Calendar.FRIDAY -> "piątek"
            Calendar.SATURDAY -> "sobotę"
            Calendar.SUNDAY -> "niedzielę"
            else -> throw IllegalArgumentException()
        }
    }

    private fun postWrongPasswordNotification(notificationHelper: NotificationHelper, prefs: MobiregPreferences) {
        val contentIntent = PendingIntent.getActivity(applicationContext, 0,
                Intent(applicationContext, MainActivity::class.java)
                        .apply { action = MainActivity.ACTION_UPDATE_PASSWORD }, 0)

        notificationHelper.postNotification(
                NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_APP_STATE)
                        .setSmallIcon(R.drawable.ic_error)
                        .setContentTitle("Mobireg – wystąpił błąd autoryzacji")
                        .setContentText(when (prefs.sex) {
                            "M" -> "Prawdopodobnie zmieniłeś hasło"
                            "K" -> "Prawdopodobnie zmieniłaś hasło"
                            else -> "Prawdopodobnie zmieniono hasło"
                        })
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_ERROR)
                        .setSubText("Dotknij, aby je wpisać")
                        .setContentIntent(contentIntent)
        )
    }


    private fun getKotlinExceptionMessage(e: Exception)
            : String = e.stackTrace.last { it?.fileName?.endsWith(".kt") == true }
            ?.run { "${e.message}: $fileName ($lineNumber)" } ?: "unknown file"

    private fun makeErrorNotification(message: String) = NotificationCompat.Builder(applicationContext,
            NotificationHelper.CHANNEL_APP_STATE)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("Mobishit – nie można odświeżyć")
            .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle("Mobishit – nie można odświeżyć"))
            .setContentText(message)!!

    private fun NotificationCompat.Builder.setDefaultsIf(condition: Boolean)
            : NotificationCompat.Builder = apply { if (condition) setDefaults(NotificationCompat.DEFAULT_ALL) }

    private fun String.fromHtml(): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(this)
        }
    }
}