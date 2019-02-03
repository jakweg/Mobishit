package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.ConnectivityManager
import android.util.Base64
import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.TestData
import jakubweg.mobishit.helper.DateHelper
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregPreferences
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

class TestsModel(application: Application)
    : BaseViewModel(application) {

    companion object {
        private const val REFRESH_TESTS_FREQUENCY_IN_MILLIS = 60 * 60 * 1000

        const val STATUS_UNKNOWN = 0
        const val STATUS_DONE = 1
        const val STATUS_DOWNLOADING = 2

        const val STATUS_ERROR_NOT_ALLOWED = 11
        const val STATUS_ERROR_CONNECTION_ERROR = 12
        const val STATUS_ERROR_SERVER_ERROR = 13
        const val STATUS_ERROR_MALFORMED_RESPONSE = 14
        const val STATUS_ERROR_UNKNOWN = 19
    }

    private fun downloadTests(): Int {
        try {
            val prefs = MobiregPreferences.get(context)
            if (!prefs.allowedInstantNotifications)
                return STATUS_ERROR_NOT_ALLOWED

            val connected = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.activeNetworkInfo?.isConnected == true

            if (!connected)
                return STATUS_ERROR_CONNECTION_ERROR

            val body = Jsoup
                    .connect(makeLinkToDownloadFrom(prefs) ?: return STATUS_ERROR_CONNECTION_ERROR)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute()
                    .body()

            if (body.isNullOrBlank())
                throw IllegalStateException("Server returned empty body")

            val jo = JsonParser()
                    .parse(body)
                    .asJsonObject

            val success = jo["success"]?.asBoolean ?: false
            val tests = jo["tests"]?.asJsonArray

            if (!success)
                return STATUS_ERROR_SERVER_ERROR

            val outputTests = mutableListOf<TestData>()
            tests?.forEach {
                require(it.isJsonObject)
                it.asJsonObject.also { obj ->
                    outputTests.add(TestData(
                            obj["id"]!!.asString.toInt(),
                            obj["date"]!!.asString,
                            obj["group"]!!.asString,
                            obj["subject"]!!.asString,
                            obj["type"]!!.asString,
                            obj["description"]!!.asString,
                            obj["add_time"]!!.asString,
                            obj["teacher"]!!.asString))
                }
            }


            AppDatabase.getAppDatabase(context)
                    .schoolTestsDao.apply {
                deleteAll()
                insertTests(outputTests)
            }
            postTests(outputTests.sortedByDescending { it.date })
            prefs.lastTestRefreshTime = System.currentTimeMillis()

            return STATUS_DONE
        } catch (mje: MalformedJsonException) {
            return STATUS_ERROR_MALFORMED_RESPONSE
        } catch (e: Exception) {
            e.printStackTrace()
            return STATUS_ERROR_UNKNOWN
        }
    }

    private fun makeLinkToDownloadFrom(prefs: MobiregPreferences): String? {
        return (DedicatedServerManager(context).testsLink ?: return null) + (
                "?l=" + URLEncoder.encode(Base64.encodeToString(prefs.loginAndHostIfNeeded.toByteArray(), Base64.DEFAULT), "UTF-8") +
                        "&h=" + URLEncoder.encode(Base64.encodeToString(prefs.host?.toByteArray(), Base64.DEFAULT), "UTF-8") +
                        "&p=" + URLEncoder.encode(Base64.encodeToString(prefs.password?.toByteArray(), Base64.DEFAULT), "UTF-8") +
                        // we add this because of multiuser accounts need them
                        "&n=" + URLEncoder.encode(Base64.encodeToString(prefs.name.toByteArray(), Base64.DEFAULT), "UTF-8") +
                        "&s=" + URLEncoder.encode(Base64.encodeToString(prefs.surname.toByteArray(), Base64.DEFAULT), "UTF-8") +
                        "&v=" + URLEncoder.encode(Base64.encodeToString(BuildConfig.VERSION_CODE.toString().toByteArray(), Base64.DEFAULT), "UTF-8") +
                        "&c=" + URLEncoder.encode(Base64.encodeToString((Locale.getDefault()?.language
                        ?: "_").toByteArray(), Base64.DEFAULT), "UTF-8"))
    }

    private var mStatus = MutableLiveData<Int>().apply { value = STATUS_UNKNOWN }

    val status get() = mStatus.asImmutable

    private val mTests = MutableLiveData<List<TestData>>()

    val tests get() = handleBackground(mTests).asImmutable

    var firstInPastIndex = -1

    private fun postTests(tests: List<TestData>) {
        val now = DateHelper.getNowDateMillis()
        when {
            tests.size == 1 -> {
                val first = tests.first()
                if (DateHelper.stringDateToMillis(first.date) < now) {
                    firstInPastIndex = 0
                    first.isInPast = true
                } else {
                    firstInPastIndex = -1
                    first.isInPast = false
                }
            }
            tests.isNotEmpty() -> {
                firstInPastIndex = tests.indexOfFirst { DateHelper.stringDateToMillis(it.date) < now }
                tests.forEachIndexed { i, it -> it.isInPast = i >= firstInPastIndex }
            }
        }
        mTests.postValue(tests)
    }

    fun considerRefreshingData() {
        if (MobiregPreferences.get(context).lastTestRefreshTime
                + REFRESH_TESTS_FREQUENCY_IN_MILLIS < System.currentTimeMillis()) {
            if ((context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager?)
                            ?.activeNetworkInfo?.isConnected == true)
                refreshDataFromInternet()
        }
    }

    private var shouldNowDownloadData = false
    fun refreshDataFromInternet() {
        shouldNowDownloadData = true
        cancelLastTask()
        handleBackground()
    }

    override fun doInBackground() {
        if (shouldNowDownloadData) {
            shouldNowDownloadData = false
            mStatus.postValue(STATUS_DOWNLOADING)
            mStatus.postValue(downloadTests())
        } else {
            val tests = AppDatabase.getAppDatabase(context)
                    .schoolTestsDao.getAllTests()
            postTests(tests)
        }
    }
}