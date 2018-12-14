package jakubweg.mobishit.service

import android.content.Context
import androidx.work.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.SentMessageData
import jakubweg.mobishit.db.getBoolean
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregPreferences
import java.util.concurrent.TimeUnit

class MessageUploadWorker(context: Context,
                          workerParams: WorkerParameters)
    : Worker(context, workerParams) {

    companion object {
        fun requestMessageSent(context: Context,
                               recipientId: Int,
                               subject: String,
                               content: String) {
            assert(subject.length in 1..120 && content.length in 1..2000)

            val messageDao = AppDatabase
                    .getAppDatabase(context).messageDao

            val insertedElementId = messageDao.insertSentRequest(
                    SentMessageData(0, subject, content,
                            System.currentTimeMillis(), recipientId, SentMessageData.STATUS_ENQUEUED))

            val data = Data
                    .Builder()
                    .putLong("id", insertedElementId)
                    .build()

            val constraints = Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request = OneTimeWorkRequest
                    .Builder(MessageUploadWorker::class.java)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .addTag("msg")
                    .addTag("m$insertedElementId")
                    .keepResultsForAtLeast(1L, TimeUnit.DAYS)
                    .build()

            WorkManager.getInstance()
                    .enqueue(request)

        }
    }

    override fun doWork(): Result {
        val messageDao = AppDatabase
                .getAppDatabase(applicationContext).messageDao
        val id = inputData.keyValueMap["id"] as Long
        return try {
            val prefs = MobiregPreferences.get(applicationContext)
            if (!prefs.isSignedIn) {
                messageDao.markMessageStatus(id, SentMessageData.STATUS_FAILED)
                return Result.failure()
            }
            messageDao.markMessageStatus(id, SentMessageData.STATUS_IN_PROGRESS)

            val messageToSentBy = messageDao.getMessageToSentById(id)

            var jo = JsonObject()
            jo.addProperty("subject", messageToSentBy.subject)
            jo.addProperty("content", messageToSentBy.content)
            jo.addProperty("recipientId", messageToSentBy.receiverId)
            jo.addProperty("login", prefs.loginAndHostIfNeeded)
            jo.addProperty("host", prefs.host)
            jo.addProperty("pass", prefs.password)
            jo.addProperty("version", BuildConfig.VERSION_CODE)

            val response = DedicatedServerManager.makePostRequest(
                    DedicatedServerManager(applicationContext).messagesLink
                    , jo.toString().toByteArray())


            jo = JsonParser().parse(response)!!.asJsonObject!!

            var returnedValue = Result.failure()
            if (jo.getBoolean("success", false))
                returnedValue = Result.success()

            if (jo.getBoolean("retry", false))
                returnedValue = Result.retry()

            if (returnedValue is Result.Success) {
                messageDao.markMessageStatus(id, SentMessageData.STATUS_SUCCEEDED)
            } else {
                messageDao.markMessageStatus(id, SentMessageData.STATUS_FAILED)
            }

            returnedValue
        } catch (e: Exception) {
            e.printStackTrace()
            messageDao.markMessageStatus(id, SentMessageData.STATUS_FAILED)
            Result.failure()
        }
    }
}