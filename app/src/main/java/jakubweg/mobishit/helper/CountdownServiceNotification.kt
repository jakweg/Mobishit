@file:Suppress("NOTHING_TO_INLINE")

package jakubweg.mobishit.helper

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.EventDao
import jakubweg.mobishit.service.CountdownService

abstract class CountdownServiceNotification private constructor(context: Context) {
    companion object {
        fun create(context: Context): CountdownServiceNotification {
            return NotificationServiceImpl(context)
        }

        private const val STATUS_NONE: Byte = 0
        private const val STATUS_BEFORE_LESSONS: Byte = 1
        private const val STATUS_DURING_LESSON: Byte = 2
        private const val STATUS_BETWEEN_LESSONS: Byte = 3
    }

    private val mHelper = NotificationHelper(context)
    private var mPreviousLesson: EventDao.CountdownServiceLesson? = null
    private var mNotificationId: Int
    val notificationId get() = mNotificationId

    init {
        mHelper.createNotificationChannels()
        mNotificationId = mHelper.getNotificationId()
    }

    protected val cancelTodayIntent by lazy(LazyThreadSafetyMode.NONE) {
        PendingIntent.getService(mHelper.context, 1, Intent(mHelper.context, CountdownService::class.java).also {
            it.putExtra("cancelToday", true)
        }, PendingIntent.FLAG_UPDATE_CURRENT)!!
    }

    private fun getTimetablePendingIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).also {
            it.action = MainActivity.ACTION_SHOW_TIMETABLE
        }, 0)!!
    }

    protected val mBuilder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.av_timer)
            .setContentTitle("Przygotowywanie odliczania")
            .setColor(ContextCompat.getColor(context, R.color.countdownNotificationBg))
            .setColorized(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setGroup("countdown") //don't group with others
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setContentIntent(getTimetablePendingIntent(context))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)!!

    private var mStatus = STATUS_NONE
    var nextDelayMillis: Long = 100L

    // ---- before lessons
    fun updateBeforeLessons(firstLesson: EventDao.CountdownServiceLesson, nowSeconds: Int) {
        if (mStatus != STATUS_BEFORE_LESSONS) {
            initBeforeLessons(firstLesson, nowSeconds)
            mStatus = STATUS_BEFORE_LESSONS
        }

        setBeforeLessonsSecond(firstLesson.startSeconds - nowSeconds)
    }

    protected abstract fun initBeforeLessons(firstLesson: EventDao.CountdownServiceLesson, nowSeconds: Int)

    protected abstract fun setBeforeLessonsSecond(remain: Int)


    // ---- during lesson
    fun updateDuringLesson(lesson: EventDao.CountdownServiceLesson,
                           nowSeconds: Int,
                           nextLesson: EventDao.CountdownServiceLesson?) {
        if (mStatus != STATUS_DURING_LESSON
                || lesson != mPreviousLesson) {
            initDuringLesson(lesson, nowSeconds, nextLesson)
            mPreviousLesson = lesson
            mStatus = STATUS_DURING_LESSON
        }

        setDuringLessonSecond(lesson, nowSeconds)
    }

    protected abstract fun initDuringLesson(lesson: EventDao.CountdownServiceLesson,
                                            nowSeconds: Int,
                                            nextLesson: EventDao.CountdownServiceLesson?)

    protected abstract fun setDuringLessonSecond(lesson: EventDao.CountdownServiceLesson, nowSeconds: Int)


    // ---- during break
    fun updateBetweenLessons(
            previousLesson: EventDao.CountdownServiceLesson,
            nextLesson: EventDao.CountdownServiceLesson,
            nowSeconds: Int) {

        if (mStatus != STATUS_BETWEEN_LESSONS
                || previousLesson != mPreviousLesson) {
            initBetweenLessons(previousLesson, nextLesson, nowSeconds)
            mPreviousLesson = previousLesson
            mStatus = STATUS_BETWEEN_LESSONS
        }

        setBetweenLessonsSecond(previousLesson.endSeconds, nextLesson.startSeconds, nowSeconds)
    }

    protected abstract fun initBetweenLessons(previousLesson: EventDao.CountdownServiceLesson,
                                              nextLesson: EventDao.CountdownServiceLesson,
                                              nowSeconds: Int)

    protected abstract fun setBetweenLessonsSecond(previousLessonEnd: Int, nextLessonStart: Int, nowSeconds: Int)


    protected inline fun setProgress(current: Int, max: Int) {
        mBuilder.setProgress(max, current, false)
    }

    protected inline fun resetProgress() {
        mBuilder.setProgress(0, 0, false)
    }


    fun postSelf(): Notification {
        val n = mBuilder.build()!!
        //n.flags = n.flags or NotificationCompat.FLAG_NO_CLEAR
        mHelper.postNotification(mNotificationId, n)
        return n
    }

    fun cancelSelf() {
        mHelper.cancelNotification(mNotificationId)
    }


    protected fun formatTime(firstLetter: Char, seconds: Int): String {
        return if (seconds < 100) {
            nextDelayMillis = 1000L
            when {
                seconds == 1 -> "${firstLetter}ozostała jedna sekunda"
                shouldUseSingleNoun(seconds) -> "${firstLetter}ozostały $seconds sekundy"
                else -> "${firstLetter}ozostało $seconds sekund"
            }
        } else {
            nextDelayMillis = 45 * 1000
            val minutes = seconds / 60
            when {
                minutes == 1 -> "${firstLetter}ozostała jedna minuta"
                shouldUseSingleNoun(minutes) -> "${firstLetter}ozostały $minutes minuty"
                else -> "${firstLetter}ozostało $minutes minut"
            }
        }
    }

    /// @return true if use 2 minuty false if 5 minut
    private fun shouldUseSingleNoun(number: Int) = if (number > 21) number % 10 in 2..4
    else number in 2..4

    protected fun buildListFromNotNullObjects(p0: Any?, p1: Any?): String {
        return if (p0 != null) {
            if (p1 != null)
                "$p0 \u2022 $p1"
            else
                p0.toString()
        } else
            p1?.toString() ?: ""
    }

    private class NotificationServiceImpl(
            context: Context,
            private val showProgressBar: Boolean,
            private val showSubText: Boolean)
        : CountdownServiceNotification(context) {

        constructor(context: Context) : this(
                context,
                true,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        private var mTempText = ""


        // ---- brefore
        override fun initBeforeLessons(firstLesson: EventDao.CountdownServiceLesson, nowSeconds: Int) {
            mTempText = if (firstLesson.roomName != null)
                " \u2022 ${firstLesson.roomName}"
            else
                ""
            clearButtons()

            mBuilder.setContentTitle("Dziś zaczniesz z ${firstLesson.name?.takeUnless { it.isBlank() }
                    ?: "pewną lekcją"}")
            resetProgress()

            if (showSubText) {
                mBuilder.setSubText(buildListFromNotNullObjects(firstLesson.name, firstLesson.roomName))
            } else
                mBuilder.setSubText(null)
        }

        override fun setBeforeLessonsSecond(remain: Int) {
            if (showSubText || mTempText.isEmpty()) {
                mBuilder.setContentText(formatTime('P', remain))
            } else {
                mBuilder.setContentText(formatTime('P', remain) + mTempText)
            }
        }


        private var nextLesson: EventDao.CountdownServiceLesson? = null
        // ---- during
        override fun initDuringLesson(lesson: EventDao.CountdownServiceLesson,
                                      nowSeconds: Int,
                                      nextLesson: EventDao.CountdownServiceLesson?) {
            resetProgress()
            clearButtons()
            isShowingNextLesson = false
            mBuilder.setContentTitle("Trwa ${lesson.name?.takeIf { it.isNotBlank() }
                    ?: "pewna lekcja"}")

            if (lesson.endSeconds - lesson.startSeconds > 45 * 60) {
                setUpCancelTodayButton()
            }

            this.nextLesson = nextLesson
            if (nextLesson == null)
                mBuilder.setSubText(null)

            if (showSubText) {
                mBuilder.setSubText(buildListFromNotNullObjects(lesson.name, lesson.roomName))
            } else {
                mTempText = if (lesson.roomName == null)
                    ""
                else
                    " \u2022 ${lesson.roomName}"
            }
        }

        private var isShowingNextLesson = false
        override fun setDuringLessonSecond(lesson: EventDao.CountdownServiceLesson, nowSeconds: Int) {
            val next = this.nextLesson
            if (next != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    nowSeconds + 5 * 60 >= next.startSeconds - (next.startSeconds - lesson.endSeconds)) {
                mBuilder.setSubText("Następnie ${buildListFromNotNullObjects(next.name, next.roomName)}")
                if (!showSubText) {
                    resetProgress()
                    isShowingNextLesson = true
                }
                this.nextLesson = null
            }

            val total = lesson.endSeconds - lesson.startSeconds
            val progress = nowSeconds - lesson.startSeconds
            mBuilder.setContentText(if (showSubText)
                formatTime('P', lesson.endSeconds - nowSeconds)
            else
                formatTime('P', lesson.endSeconds - nowSeconds) + mTempText)

            if (!isShowingNextLesson && showProgressBar)
                setProgress(progress, total)
        }


        // ----- between
        override fun initBetweenLessons(previousLesson: EventDao.CountdownServiceLesson,
                                        nextLesson: EventDao.CountdownServiceLesson,
                                        nowSeconds: Int) {
            clearButtons()
            if (nextLesson.startSeconds - previousLesson.endSeconds > 30 * 60) {
                setUpCancelTodayButton()
            }

            if (showSubText) {
                mBuilder.setContentTitle("Wkrótce ${nextLesson.name}")
                mBuilder.setSubText(if (nextLesson.roomName != null)
                    "Następnie ${nextLesson.name} w ${nextLesson.roomName}"
                else
                    "Następnie ${nextLesson.name}")

            } else {
                mBuilder.setSubText(null)
                mTempText = if (nextLesson.roomName != null)
                    " \u2022 ${nextLesson.roomName} \u2022 ${nextLesson.name}"
                else
                    " \u2022 ${nextLesson.name}"

                mBuilder.setContentTitle("Wkrótce ${nextLesson.name}")
            }
        }

        override fun setBetweenLessonsSecond(previousLessonEnd: Int, nextLessonStart: Int, nowSeconds: Int) {
            val total = nextLessonStart - previousLessonEnd
            val progress = (nowSeconds - previousLessonEnd)
            mBuilder.setContentText(if (showSubText)
                formatTime('P', total - progress)
            else
                formatTime('P', total - progress) + mTempText)
            if (showProgressBar)
                setProgress(progress, total)
        }

        private fun clearButtons() {
            mBuilder.mActions.clear()
        }

        private fun setUpCancelTodayButton() {
            if (mBuilder.mActions.isNotEmpty())
                return
            mBuilder.mActions.clear()
            mBuilder.addAction(R.drawable.ic_timer_off, "Ukryj dzisiaj", cancelTodayIntent)
        }
    }
}