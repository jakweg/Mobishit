package jakubweg.mobishit.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.EventDao
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*


class TimetableWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000
        fun getDayMillis(context: Context): Long {
            val cal = Calendar.getInstance()!!
            var millis = DateHelper.getNowDateMillis()

            val hour = cal[Calendar.HOUR_OF_DAY]
            if (hour >= 16) {
                millis += MILLIS_IN_DAY
            }

            val dao = AppDatabase.getAppDatabase(context).eventDao
            var maxDays = 5
            while (maxDays-- > 0) {
                if (dao.hasEventsForDay(millis)) {
                    break
                }
                millis += MILLIS_IN_DAY
            }
            if (maxDays <= 0) {
                return System.currentTimeMillis()
            }
            return millis
        }

        private var cachedLessons: List<EventDao.EventShortInfo>? = null

        fun requestInstantUpdate(context: Context) {
            cachedLessons = null
            MobiregPreferences.get(context).hasReadyWidgetCache = false

            val intent = Intent(context, TimetableWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            val manager = AppWidgetManager.getInstance(context.applicationContext)

            val ids = manager.getAppWidgetIds(ComponentName(context.applicationContext, TimetableWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)

            manager.notifyAppWidgetViewDataChanged(ids, R.id.lessonsList)
        }

        fun requestAutoUpdates(context: Context) {
            val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, WidgetUpdateReceiver::class.java)
            intent.action = "fullUpdate"

            val operation = PendingIntent.getBroadcast(context, 0, intent, 0) ?: return

            val millis = Calendar.getInstance().run {
                if (get(Calendar.HOUR_OF_DAY) >= 17)
                    add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 17)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)

                return@run timeInMillis
            }

            mgr.setInexactRepeating(AlarmManager.RTC, millis, AlarmManager.INTERVAL_DAY, operation)
        }
    }

    class WidgetUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            requestInstantUpdate(context)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        try {
            val rv = RemoteViews(context.packageName, R.layout.widget_initial_layout)

            val isLogged = MobiregPreferences.get(context).isSignedIn

            if (isLogged) {
                val intent = Intent(context, ListWidgetRemoteService::class.java)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

                rv.setTextViewText(R.id.timetableSubtitle, DateHelper.formatPrettyDate(getDayMillis(context)))

                rv.setRemoteAdapter(R.id.lessonsList, intent)

                rv.setViewVisibility(R.id.errorBtn, View.GONE)

                rv.setEmptyView(R.id.lessonsList, R.id.empty_view)

                rv.setOnClickPendingIntent(R.id.timetableTitle,
                        PendingIntent.getActivity(context, 0,
                                Intent(context, MainActivity::class.java).also {
                                    it.action = MainActivity.ACTION_SHOW_TIMETABLE
                                }, 0))
            } else {

                val intent = PendingIntent.getActivity(context, 0,
                        Intent(context, MainActivity::class.java), 0)

                rv.setTextViewText(R.id.errorBtn, "Nie zalogowano do Mobirega, kliknij aby się zalogować")
                rv.setOnClickPendingIntent(R.id.errorBtn, intent)
            }

            appWidgetManager.updateAppWidget(widgetId, rv)
        } catch (e: Exception) {
            Log.e("TimetableWidgetProvider", "Wystąpił błąd: ${e.localizedMessage}", e)
        }
    }

    class ListWidgetRemoteService : RemoteViewsService() {
        override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
            return ListRemoteViewsFactory(applicationContext)
        }
    }

    private class ListRemoteViewsFactory(val appContext: Context) : RemoteViewsService.RemoteViewsFactory {

        private var events = listOf<EventDao.EventShortInfo>()
        override fun onCreate() {}

        override fun onDestroy() = Unit

        override fun getCount() = events.size

        override fun getViewTypeCount() = 1

        override fun getItemId(position: Int) = position.toLong()

        override fun hasStableIds() = true

        override fun getLoadingView(): RemoteViews? = null

        private var showLessonNumber = false

        override fun onDataSetChanged() {
            val prefs = MobiregPreferences.get(appContext)
            showLessonNumber = prefs.showLessonNumberOnWidget
            if (prefs.hasReadyWidgetCache) {
                cachedLessons.also {
                    if (it != null)
                        events = it
                    else {
                        val tmp = getDataFromCachedFile(appContext)
                        if (tmp == null) {
                            events = getDataFromDb(appContext)
                            saveCache(appContext)
                        } else events = tmp

                        cachedLessons = events
                    }
                }
            } else {
                events = getDataFromDb(appContext)
                saveCache(appContext)
                cachedLessons = events
                prefs.hasReadyWidgetCache = true
            }
        }

        private fun saveCache(context: Context) {
            try {
                ObjectOutputStream(File(context.cacheDir, "widget")
                        .outputStream()).use {
                    it.write(events.size)
                    events.forEach(it::writeObject)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getDataFromCachedFile(context: Context): List<EventDao.EventShortInfo>? {
            try {
                val file = File(context.cacheDir, "widget")
                if (!file.exists()) return null

                ObjectInputStream(file.inputStream()).use {
                    val count = it.readInt()
                    val list = mutableListOf<EventDao.EventShortInfo>()

                    for (i in 0 until count)
                        list.add(it.readObject() as EventDao.EventShortInfo)

                    return list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun getDataFromDb(context: Context): List<EventDao.EventShortInfo> {
            val dao = AppDatabase.getAppDatabase(context).eventDao

            return dao.getShortEventsInfoByDay(getDayMillis(context))
        }

        override fun getViewAt(position: Int) =
                RemoteViews(appContext.packageName, R.layout.widget_list_item).apply {
                    try {
                        events[position].also {
                            if (showLessonNumber) {
                                setTextViewText(R.id.lessonNumber, it.number?.toString() ?: "-")
                                setViewVisibility(R.id.lessonNumber, View.VISIBLE)
                            } else
                                setViewVisibility(R.id.lessonNumber, View.GONE)
                            setTextViewText(R.id.lessonHour, it.time.replace('|', '\n'))
                            setTextViewText(R.id.lessonName, buildString {
                                append(it.subjectName ?: "Nieznana lekcja")
                                if (!it.roomName.isNullOrBlank()) {
                                    append(' ')
                                    append(it.roomName)
                                }
                            })
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
    }

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        context ?: return
        appWidgetManager ?: return
        appWidgetIds ?: return
        try {
            appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
        } catch (e: Exception) {
            Log.e("TimetableWidgetProvider", "Error while updating widget", e)
        }
    }
}