package jakubweg.mobishit.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jakubweg.mobishit.db.asStringOrNull
import jakubweg.mobishit.helper.DedicatedServerManager
import org.jsoup.HttpStatusException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

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

            val url = URL(DedicatedServerManager(applicationContext).crashReportsLink)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 20 * 1000
                requestMethod = "POST"
                doInput = true
                doOutput = true
            }


            val os = connection.outputStream

            BufferedOutputStream(os).use {
                it.write(jo.toString().toByteArray(Charsets.UTF_8))
            }

            os.close()

            val bis = BufferedInputStream(connection.inputStream)
            val buf = ByteArrayOutputStream()

            val buffer = ByteArray(1024 * 8)
            var read = 0
            while (read >= 0) {
                read = bis.read(buffer)
                if (read == -1)
                    break
                buf.write(buffer, 0, read)
                if (read == 0)
                    Thread.sleep(10L)
            }

            bis.close()
            connection.disconnect()

            val response = String(buf.toByteArray(), Charsets.UTF_8)

            jo = JsonParser()
                    .parse(response)
                    .asJsonObject!!

            var returnedValue = Result.FAILURE
            if (jo["success"]?.let {
                        (it.isJsonPrimitive && it.asBoolean)
                                || it.asStringOrNull == "true"
                    } == true)
                returnedValue = Result.SUCCESS

            if (jo["retry"]?.let {
                        (it.isJsonPrimitive && it.asBoolean)
                                || it.asStringOrNull == "true"
                    } == true)
                returnedValue = Result.RETRY

            returnedValue //retry
        } catch (hse: HttpStatusException) {
            Result.RETRY
        } catch (ste: SocketTimeoutException) {
            Result.RETRY
        } catch (e: Exception) {
            e.printStackTrace()
            Result.FAILURE
        }
    }
}