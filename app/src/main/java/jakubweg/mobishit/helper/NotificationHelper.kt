@file:Suppress("MemberVisibilityCanBePrivate")

package jakubweg.mobishit.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import jakubweg.mobishit.BuildConfig

@Suppress("NOTHING_TO_INLINE")
class NotificationHelper(val context: Context) {
    private val mService = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    companion object {
        private const val CHANNEL_PREFIX = "channels."
        const val CHANNEL_APP_STATE = CHANNEL_PREFIX + "app"
        const val CHANNEL_MARKS = CHANNEL_PREFIX + "mark"
        const val CHANNEL_MESSAGES = CHANNEL_PREFIX + "message"
        const val CHANNEL_ATTENDANCES = CHANNEL_PREFIX + "attendance"
        const val CHANNEL_SUBSTITUTIONS = CHANNEL_PREFIX + "substitutions"
        const val CHANNEL_COUNTDOWN = CHANNEL_PREFIX + "countdown"

        const val CHANNEL_GROUP_MOBIREG = CHANNEL_PREFIX + "groups." + "mobireg"

        private val mutex = Any()
    }

    fun getNotificationId(): Int {
        synchronized(mutex) {
            val preferences = context
                    .getSharedPreferences("notifications", Context.MODE_PRIVATE)!!

            val next = preferences.getInt("nextId", 1)
            preferences.edit().putInt("nextId", next + 1).apply()

            return next
        }
        //return getNotificationIds(1).first()
    }

    fun getNotificationIds(count: Int): Array<Int> {
        assert(count > 0)
        synchronized(mutex) {

            val preferences = context
                    .getSharedPreferences("notifications", Context.MODE_PRIVATE)!!

            var next = preferences.getInt("nextId", 1)
            preferences.edit().putInt("nextId", next + count).apply()

            return Array(count) { next++ }
        }
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        synchronized(mutex) {
            val preferences = context
                    .getSharedPreferences("notifications", Context.MODE_PRIVATE)!!

            if (preferences.getInt("channelsState", 0) != 1) {
                internalCreateChannels()
                preferences.edit().putInt("channelsState", 1).apply()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun internalCreateChannels() {
        val groups = mutableListOf(
                NotificationChannelGroup(CHANNEL_GROUP_MOBIREG, "Powiadomienia z dziennika")
        )

        val channels = mutableListOf(
                makeChannel(CHANNEL_APP_STATE, "Powiadomienia systemowe", NotificationManager.IMPORTANCE_LOW),
                makeChannel(CHANNEL_COUNTDOWN, "Odliczanie do końca lekcji", NotificationManager.IMPORTANCE_LOW, showBadges = false),
                makeChannel(CHANNEL_MARKS, "Nowe oceny", NotificationManager.IMPORTANCE_DEFAULT, CHANNEL_GROUP_MOBIREG),
                makeChannel(CHANNEL_MESSAGES, "Nowe wiadomości", NotificationManager.IMPORTANCE_DEFAULT, CHANNEL_GROUP_MOBIREG),
                makeChannel(CHANNEL_ATTENDANCES, "Obecności, nieobeconości i spóźnienia", NotificationManager.IMPORTANCE_DEFAULT, CHANNEL_GROUP_MOBIREG),
                makeChannel(CHANNEL_SUBSTITUTIONS, "Zastępstwa i odwołane lekcje", NotificationManager.IMPORTANCE_DEFAULT, CHANNEL_GROUP_MOBIREG)
        )


        mService.createNotificationChannelGroups(groups)
        mService.createNotificationChannels(channels)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun makeChannel(channelId: String, name: CharSequence, importance: Int, groupId: String? = null, showBadges: Boolean = true)
            : NotificationChannel = NotificationChannel(channelId, name, importance).apply {
        if (groupId != null) group = groupId
        setShowBadge(showBadges)
    }

    fun postNotification(id: Int, notification: NotificationCompat.Builder, isCancelable: Boolean = true) {
        val n = notification.build()
        if (!isCancelable)
            n.flags = n.flags or Notification.FLAG_NO_CLEAR
        else
            n.flags = n.flags or Notification.FLAG_AUTO_CANCEL

        postNotification(id, n)
    }

    fun isChannelMuted(channel: String?) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        mService.getNotificationChannel(channel).importance == NotificationManager.IMPORTANCE_NONE
    else false

    fun postNotification(notification: NotificationCompat.Builder) = postNotification(getNotificationId(), notification)

    fun postNotification(id: Int, notification: Notification) {
        if (BuildConfig.DEBUG) {
            notification.apply {
                Log.v("NotificationHelper", "Posting notification on channel $channelCompat")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                mService.notify(id, notification)
            } catch (e: Exception) {
                e.printStackTrace()
                // probably channels not created
                internalCreateChannels()
                mService.notify(id, notification)
            }
        } else mService.notify(id, notification)
    }

    fun cancelNotification(id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
    }

    private inline val Notification.channelCompat
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) "null" else channelId ?: "null"


}