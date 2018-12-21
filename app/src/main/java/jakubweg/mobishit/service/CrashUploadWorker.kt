package jakubweg.mobishit.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jakubweg.mobishit.db.getBoolean
import jakubweg.mobishit.helper.DedicatedServerManager
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException

class CrashUploadWorker(context: Context,
                        workerParams: WorkerParameters) :
        Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            val data = inputData.keyValueMap

            var jo = JsonObject()
            data.forEach {
                jo.addProperty(it.key, it.value.toString())
            }

            val response = DedicatedServerManager
                    .makePostRequest(
                            DedicatedServerManager(applicationContext).crashReportsLink
                                    ?: return Result.failure(),
                            jo.toString().toByteArray(Charsets.UTF_8))

            jo = JsonParser()
                    .parse(response)
                    .asJsonObject!!

            var returnedValue = Result.failure()
            if (jo.getBoolean("success", false))
                returnedValue = Result.success()

            if (jo.getBoolean("retry", false))
                returnedValue = Result.retry()

            returnedValue
        } catch (hse: HttpStatusException) {
            Result.retry()
        } catch (ste: SocketTimeoutException) {
            Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}