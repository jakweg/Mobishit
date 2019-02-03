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
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.activity.SettingsActivity
import jakubweg.mobishit.service.CrashUploadWorker
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object CrashHandler {
    private val Throwable.isRoomDatabaseError: Boolean
        get() = (this is IllegalStateException
                && toString().run {
            contains("Room cannot verify the data integrity", true)
                    || contains("SQLiteDatabase", true)
        }) || (this.cause?.let { this != it && it.isRoomDatabaseError } == true)

    fun onNewCrash(context: Context,
                   thread: Thread,
                   exception: Throwable) {
        try {
            if (exception.isRoomDatabaseError) {
                // we drop database and request new one
                SettingsMigrationHelper.deleteDatabaseAndRequestNew(MobiregPreferences.get(context).prefs, context)

                Thread.sleep(2000L) //give some time to WorkManager to enqueue work

                AlarmManagerCompat.setExactAndAllowWhileIdle(
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 3000L,
                        PendingIntent.getActivity(context, Random.nextInt(-1000, 0),
                                Intent(context, MainActivity::class.java), PendingIntent.FLAG_ONE_SHOT))

                System.exit(1)
                return
            }

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

            Thread.sleep(1500) // give some time to WorkManager for enqueuing request

            if (!MobiregPreferences.get(context).ignoreCrashes)
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1500,
                        PendingIntent.getActivity(context, 0,
                                Intent(context, SettingsActivity::class.java).also {
                                    it.action = SettingsActivity.ACTION_SHOW_CRASH_DIALOG
                                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }, PendingIntent.FLAG_ONE_SHOT)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        System.exit(1)
    }

    private fun Throwable.stackTraceToString(): String {
        val errors = StringWriter()
        this.printStackTrace(PrintWriter(errors))
        return errors.toString()
    }
}