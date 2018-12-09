package jakubweg.mobishit.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.AlarmManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.activity.SettingsActivity
import jakubweg.mobishit.service.CrashUploadWorker
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

object CrashHandler {
    fun onNewCrash(context: Context,
                   thread: Thread,
                   exception: Throwable) {
        try {
            val input = Data.Builder()
                    .putString("token", MobiregPreferences.get(context).firebaseToken)
                    .putString("thread_name", thread.name)
                    .putString("date", DateHelper.millisToStringTime(System.currentTimeMillis()))
                    .putString("version_name", BuildConfig.VERSION_NAME)
                    .putString("version_code", BuildConfig.VERSION_CODE.toString())
                    .putString("sdk_int", Build.VERSION.SDK_INT.toString())
                    .putString("brand", Build.BRAND)
                    .putString("device", Build.DEVICE)
                    .putString("manufacturer", Build.MANUFACTURER)
                    .putString("model", Build.MODEL)
                    .putString("exception_name", exception.javaClass.name)
                    .putString("stack_trace", exception.stackTraceToString())
                    .build()

            val request = OneTimeWorkRequest
                    .Builder(CrashUploadWorker::class.java)
                    .setInputData(input)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            10,
                            TimeUnit.MINUTES)
                    .addTag("crash")
                    .build()

            WorkManager.getInstance()
                    .enqueue(request)


            if (!MobiregPreferences.get(context).ignoreCrashes)
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 2 * 1000,
                        PendingIntent.getActivity(context, 0,
                                Intent(context, SettingsActivity::class.java).also {
                                    it.action = SettingsActivity.ACTION_SHOW_CRASH_DIALOG
                                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }, PendingIntent.FLAG_ONE_SHOT)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        System.exit(0)
    }

    private fun Throwable.stackTraceToString() = StringWriter().run { printStackTrace(PrintWriter(this)) }.toString()
}