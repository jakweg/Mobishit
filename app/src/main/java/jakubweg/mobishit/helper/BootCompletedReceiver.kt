package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import jakubweg.mobishit.service.CountdownService

class BootCompletedReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        MobiregPreferences.get(context).apply {
            if (!isSignedIn)
                return@apply

            if (runCountdownService)
                CountdownService.start(context)

            TimetableWidgetProvider.requestAutoUpdates(context)
        }
    }
}