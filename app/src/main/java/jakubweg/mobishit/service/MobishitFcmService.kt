package jakubweg.mobishit.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.util.Base64
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.NotificationHelper
import java.net.URLEncoder

class MobishitFcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String?) {
        MobiregPreferences.get(this)
                .firebaseToken = token
        FcmServerNotifierWorker.requestPeriodicServerNotifications()
    }

    companion object {
        private const val KEY_ACTION = "action"
        private const val KEY_IS_OFFICIAL = "official"
        private const val KEY_MIN_VERSION = "minVer"
        private const val KEY_MAX_VERSION = "maxVer"

        private const val TRUE = "true"

        private const val ACTION_REFRESH_MOBIREG = "refreshMobireg"
        private const val ACTION_CHECK_FOR_UPDATES = "checkUpdates"
        private const val ACTION_SAY_HELLO = "sayHello"
        private const val ACTION_SHOW_NOTIFICATION = "showNotification"
        private const val ACTION_RESEND_TOKEN = "resendToken"
        private const val ACTION_EXPIRE_DEDICATED_SERVER = "expireServer"

        private const val EXTRA_TITLE = "n_title"
        private const val EXTRA_CONTENT = "n_content"
        private const val EXTRA_SUB_TEXT = "n_sub"
        private const val EXTRA_FORMAT_TEXT = "n_fill_frags"
        private const val EXTRA_ACTION_ON_CLICK = "n_onclick"
        private const val EXTRA_NOTIFY_WITH_SOUND = "n_sound"
        private const val EXTRA_NOTIFICATION_CHANNEL = "n_channel"
        private const val EXTRA_NOTIFICATION_ID = "n_id"
        private const val EXTRA_NOTIFICATION_TIMEOUT = "n_timeout"

        private const val BEGIN_URL = "url:"
        private const val BEGIN_MAIN_ACTIVITY = "main_act:"

        private const val LINK_FRAG_NAME = "%name%"
        private const val LINK_FRAG_SURNAME = "%surname%"
        private const val LINK_FRAG_USERNAME = "%username%"
        private const val LINK_FRAG_HOST = "%host%"
        //private const val LINK_FRAG_PASS_HASH = "%pass%"
        private const val LINK_FRAG_SEX = "%sex%"
    }

    override fun onMessageReceived(msg: RemoteMessage?) {
        msg ?: return
        val context = applicationContext ?: return
        val body = msg.data ?: return
        val prefs = MobiregPreferences.get(context)

        val isOfficial = body[KEY_IS_OFFICIAL] == TRUE

        if (!isOfficial && !BuildConfig.DEBUG) {
            return //goodbye, we are not devs
        }

        val minVersion = body[KEY_MIN_VERSION]?.toIntOrNull() ?: -1
        val maxVersion = body[KEY_MAX_VERSION]?.toIntOrNull() ?: Int.MAX_VALUE

        if (BuildConfig.VERSION_CODE !in minVersion..maxVersion) {
            return // this app version does not match the fcm message requirements
        }

        val action = body[KEY_ACTION]
        if (action.isNullOrBlank()) {
            postErrorNotification("Got null action")
            return
        }
        if (prefs.lastCheckTime + 15 * 1000L > System.currentTimeMillis()
                && prefs.lastFcmAction == action) {
            prefs.setLastFcmAction(action)
            return //probably two same fcm messages - ignore
        }
        prefs.setLastFcmAction(action)

        when (action) {
            ACTION_REFRESH_MOBIREG -> UpdateWorker.requestUpdates(context)
            ACTION_CHECK_FOR_UPDATES -> AppUpdateWorker.requestChecks()
            ACTION_SAY_HELLO -> sayHello()
            ACTION_SHOW_NOTIFICATION -> showNotificationFromFcm(body)
            ACTION_RESEND_TOKEN -> resendToken()
            ACTION_EXPIRE_DEDICATED_SERVER -> expireDedicatedServer()

            else -> postErrorNotification("Unknown action: $action")
        }
    }


    private fun sayHello() {
        postErrorNotification("Hello there", "Hello caused by FCM")
    }

    private fun resendToken() {
        FcmServerNotifierWorker.requestPeriodicServerNotifications()
    }

    private fun expireDedicatedServer() {
        DedicatedServerManager(this).makeExpired()
    }

    private fun showNotificationFromFcm(body: Map<String, String>) {
        val fillFragments = body[EXTRA_FORMAT_TEXT] == TRUE

        val title = fillStringWithFragmentsIf(fillFragments, body[EXTRA_TITLE].takeUnless { it.isNullOrBlank() })
        val content = fillStringWithFragmentsIf(fillFragments, body[EXTRA_CONTENT].takeUnless { it.isNullOrBlank() })
        val subText = fillStringWithFragmentsIf(fillFragments, body[EXTRA_SUB_TEXT].takeUnless { it.isNullOrBlank() })

        //val uriOnClicked = fillStringWithFragments(body[EXTRA_URI_ON_CLICK], true).takeUnless { it.isNullOrBlank() }
        val onClickAction = body[EXTRA_ACTION_ON_CLICK]?.takeUnless { it.isBlank() }

        val defaults = if (body[EXTRA_NOTIFY_WITH_SOUND] == TRUE)
            NotificationCompat.DEFAULT_ALL else 0
        val timeout = body[EXTRA_NOTIFICATION_TIMEOUT]?.toLongOrNull() ?: 7 * 24 * 60 * 60 * 1000L
        val channel = body[EXTRA_NOTIFICATION_CHANNEL].takeUnless { it.isNullOrBlank() }
                ?: NotificationHelper.CHANNEL_APP_STATE

        if (title.isNullOrBlank()) {
            postErrorNotification("Can't show notification - title is null")
            return
        }

        val contentIntent = getOnClickIntent(onClickAction)


        val context = applicationContext ?: return
        NotificationHelper(context).apply {
            val id = body[EXTRA_NOTIFICATION_ID]?.toIntOrNull() ?: getNotificationId()
            createNotificationChannels()

            val notification = NotificationCompat
                    .Builder(context, channel)
                    .setSmallIcon(R.drawable.nav_info)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSubText(subText)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setStyle(NotificationCompat
                            .BigTextStyle()
                            .bigText(content))
                    .setContentIntent(contentIntent)
                    .setDefaults(defaults)
                    .setTimeoutAfter(timeout)

            postNotification(id, notification)
        }
    }

    private fun getOnClickIntent(onClickAction: String?): PendingIntent? {
        onClickAction ?: return null

        // we have to provide support for previous versions, so

        if (onClickAction.startsWith("http"))
            return getOpenLinkIntent(onClickAction)

        // now we can check by new standard

        return when {
            onClickAction.startsWith(BEGIN_URL) -> getOpenLinkIntent(onClickAction.substring(BEGIN_URL.length))
            onClickAction.startsWith(BEGIN_MAIN_ACTIVITY) -> getOpenMainActivityIntent(onClickAction.substring(BEGIN_MAIN_ACTIVITY.length))
            else -> {
                postErrorNotification("Unknown beginning of onClickAction '$onClickAction'")
                null
            }
        }
    }

    private fun getOpenLinkIntent(uriOnClicked: String): PendingIntent? {
        return try {
            PendingIntent.getActivity(applicationContext, uriOnClicked.hashCode(),
                    Intent(Intent.ACTION_VIEW, Uri.parse(fillStringWithFragments(uriOnClicked, false))), PendingIntent.FLAG_UPDATE_CURRENT)
        } catch (e: Exception) {
            postErrorNotification("Can't create pending intent: ${e.localizedMessage}")
            null
        }
    }

    private fun getOpenMainActivityIntent(action: String): PendingIntent? {
        return try {
            PendingIntent.getActivity(this, action.hashCode(), Intent(this, MainActivity::class.java).also {
                it.action = action
            }, PendingIntent.FLAG_UPDATE_CURRENT)
        } catch (e: Exception) {
            postErrorNotification("Can't create pending intent: ${e.localizedMessage}")
            null
        }
    }

    private fun fillStringWithFragmentsIf(condition: Boolean, text: String?): String? {
        return if (condition) fillStringWithFragments(text, false) else text
    }

    private fun fillStringWithFragments(text: String?, encodeBase64: Boolean): String? {
        text ?: return null

        val prefs = MobiregPreferences.get(applicationContext ?: return null)

        return text
                .replaceLazy(LINK_FRAG_USERNAME, encodeBase64) { prefs.loginAndHostIfNeeded }
                .replaceLazy(LINK_FRAG_HOST, encodeBase64) { prefs.host ?: "" }
                .replaceLazy(LINK_FRAG_NAME, encodeBase64) { prefs.name }
                .replaceLazy(LINK_FRAG_SURNAME, encodeBase64) { prefs.surname }
                .replaceLazy(LINK_FRAG_SEX, encodeBase64) { prefs.sex }
        //.replaceLazy(LINK_FRAG_PASS_HASH) { prefs.password } We shouldn't share password :\

    }

    private inline fun String.replaceLazy(oldString: String,
                                          encodeBase64: Boolean,
                                          lazyString: () -> String): String {
        return if (contains(oldString))
            replace(oldString, if (encodeBase64) encode(lazyString.invoke()) else lazyString.invoke())
        else this
    }

    private fun encode(s: String): String {
        return URLEncoder.encode(Base64.encodeToString(
                s.toByteArray(), Base64.DEFAULT), "UTF-8")!!
    }

    private fun postErrorNotification(message: String) = postErrorNotification("MobishitFcmService", message)

    private fun postErrorNotification(title: String, message: String) {
        val context = applicationContext ?: return
        NotificationHelper(context).apply {
            createNotificationChannels()

            val notification = NotificationCompat
                    .Builder(context, NotificationHelper.CHANNEL_APP_STATE)
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