@file:Suppress("NOTHING_TO_INLINE")

package jakubweg.mobishit.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.EventDao
import jakubweg.mobishit.helper.CountdownServiceNotification
import jakubweg.mobishit.helper.DateHelper
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.NotificationHelper
import java.util.*

class CountdownService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object ServiceController {
        private const val ACTION_STOP_SERVICE = "stop"

        fun startIfNeeded(context: Context) {
            val prefs = MobiregPreferences.get(context)
            if (!prefs.run { isSignedIn && runCountdownService })
                return

            val dao = AppDatabase.getAppDatabase(context).eventDao

            val today = DateHelper.getNowDateMillis()

            val startSecond = DateHelper.getSecond(dao.getFirstLessonStartTime(today) ?: return)
            val endSecond = DateHelper.getSecond(dao.getLastLessonEndTime(today) ?: return)

            val nowSecond = DateHelper.getSecondsOfNow()

            if (nowSecond >= startSecond - prefs.beforeLessonsMinutes * 60 && nowSecond < endSecond)
                start(context)
        }

        fun start(context: Context) {
            ContextCompat.startForegroundService(context.applicationContext,
                    Intent(context, CountdownService::class.java))
        }

        fun stop(context: Context) {
            LocalBroadcastManager.getInstance(context.applicationContext)
                    .sendBroadcast(Intent(ACTION_STOP_SERVICE))
        }

        private fun getServicePendingIntent(context: Context, requestCode: Int): PendingIntent {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                PendingIntent.getService(
                        context.applicationContext, requestCode, Intent(context, CountdownService::class.java), PendingIntent.FLAG_UPDATE_CURRENT)!!
            else
                PendingIntent.getForegroundService(
                        context.applicationContext, requestCode, Intent(context, CountdownService::class.java), PendingIntent.FLAG_UPDATE_CURRENT)!!
        }
    }

    private val stopListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_SERVICE) {
                synchronized(this) {
                    shouldStillRun = false
                    stopSelf()
                }
            }
        }
    }

    private val notification by lazy(LazyThreadSafetyMode.NONE) { CountdownServiceNotification.create(this) }

    private var handlerThread: HandlerThread? = null
    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(handlerThread!!.looper)
    }

    private var nextDelayMillis = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("cancelToday", false) == true) {
            requestStop(false)
            nextStartMillis = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
            MobiregPreferences.get(this)
                    .nextAllowedCountdownServiceStart = nextStartMillis
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(stopListener, IntentFilter(ACTION_STOP_SERVICE))

        startForeground(notification.notificationId, notification.postSelf())


        val prefs = MobiregPreferences.get(this)
        Log.d("xXD", "x\n${Calendar.getInstance().timeInMillis}\n${prefs.nextAllowedCountdownServiceStart}" +
                "\n${prefs.nextAllowedCountdownServiceStart <= Calendar.getInstance().timeInMillis}\n")
        if (prefs.run {
                    isSignedIn && runCountdownService &&
                            nextAllowedCountdownServiceStart <= Calendar.getInstance().timeInMillis
                }
                && !NotificationHelper(this).isChannelMuted(NotificationHelper.CHANNEL_COUNTDOWN)) {
            beforeLessonsMinutes = prefs.beforeLessonsMinutes

            handlerThread = HandlerThread("CountdownService", Process.THREAD_PRIORITY_BACKGROUND)
            handlerThread?.start()
            handler.post(prepareLessonsTask)
        } else {
            stopSelf()
        }
    }

    private var shouldStillRun = true
    private var beforeLessonsMinutes = 0
    private val updateNotificationTask = Runnable {
        try {
            updateNotification()
            nextDelayMillis = notification.nextDelayMillis
            notification.postSelf()
            synchronized(this) {
                if (shouldStillRun)
                    requestUpdates()
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Awaria w usłudze odliczania – zabijam się", Toast.LENGTH_LONG).show()
                stopSelf()
            }
            e.printStackTrace()
        }
    }

    private fun requestUpdates() {
        handler.postDelayed(updateNotificationTask, nextDelayMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(stopListener)

        if (handlerThread != null)
            handler.removeCallbacks(updateNotificationTask)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            handlerThread?.quitSafely()
        } else {
            handlerThread?.quit()
        }
        handlerThread?.join()

        val now = System.currentTimeMillis()

        if (nextStartMillis > now &&
                MobiregPreferences.get(this).run { isSignedIn && runCountdownService }) {

            val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr.set(AlarmManager.RTC, nextStartMillis, getServicePendingIntent(this, 0))
        }

        notification.cancelSelf()
        stopForeground(true)
    }

    private var lessons = Array<EventDao.CountdownServiceLesson>(0) { throw UnsupportedOperationException() }

    private var nextStartMillis = 0L

    private val prepareLessonsTask = Runnable {
        val dao = AppDatabase.getAppDatabase(this).eventDao

        val today = DateHelper.getNowDateMillis()

        lessons = dao.getCountdownServiceLessons(today).toTypedArray()

        val now = DateHelper.getSecondsOfNow()

        if (lessons.isEmpty()
                || lessons.first().startSeconds - MobiregPreferences.get(this).beforeLessonsMinutes * 60 > now) {
            requestStop()
            return@Runnable
        }

        requestUpdates()
    }

    private fun requestStop(calculateNextStart: Boolean = true) {
        synchronized(this) { shouldStillRun = false }
        if (calculateNextStart)
            calculateNextStart()
        stopSelf()
    }

    private fun calculateNextStart() {
        val nowSeconds = getSecondOfDay()

        val cal = Calendar.getInstance()

        nextStartMillis = if (lessons.isNotEmpty()) {
            val first = lessons.first()
            val last = lessons.last()
            when {
                first.startSeconds - beforeLessonsMinutes > nowSeconds -> {
                    cal.set(Calendar.HOUR_OF_DAY, first.startSeconds / 60 / 60)
                    cal.set(Calendar.MINUTE, first.startSeconds / 60 % 60)
                    cal.add(Calendar.MINUTE, -beforeLessonsMinutes)
                    cal.timeInMillis
                }
                last.endSeconds < nowSeconds -> {
                    cal.set(Calendar.HOUR_OF_DAY, 1)
                    cal.set(Calendar.MINUTE, 0)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.timeInMillis
                }
                else -> throw IllegalStateException()
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.timeInMillis
        }
    }

    private fun getSecondOfDay(): Int {
        return Calendar.getInstance().run {
            get(Calendar.HOUR_OF_DAY) * 60 * 60 +
                    get(Calendar.MINUTE) * 60 +
                    get(Calendar.SECOND)
        }
    }

    private fun updateNotification() {
        val nowSeconds = getSecondOfDay()

        assert(lessons.isNotEmpty())

        lessons.last().also { last ->
            if (last.endSeconds < nowSeconds) {
                requestStop()
                return
            }
        }

        lessons.first().also { first ->
            if (first.startSeconds > nowSeconds) {
                notification.updateBeforeLessons(first, nowSeconds)
                return
            }
        }


        lessons.lastIndexWhich { it.startSeconds <= nowSeconds }.also {
            val item = lessons[it]
            when {
                nowSeconds <= item.endSeconds -> notification.updateDuringLesson(item, nowSeconds)
                it == lessons.size - 1 -> throw IllegalStateException("it == lessons.size - 1")
                else -> notification.updateBetweenLessons(lessons[it], lessons[it + 1], nowSeconds)
            }
        }
    }

    private inline fun <reified T> Array<T>.lastIndexWhich(function: (T) -> Boolean): Int {
        var i = 0
        while (i < size) {
            if (function.invoke(this[i])) {
                i++
                while (i < size) {
                    if (!function.invoke(this[i])) {
                        break
                    }
                    i++
                }
                return i - 1
            }
            i++
        }
        throw Exception("Array.lastIndexWhich - no element which meets the requirements")
    }
}
