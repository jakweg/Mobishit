package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.JsonParser
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.ComparisonCacheData
import jakubweg.mobishit.db.TermDao
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregPreferences
import org.jsoup.Jsoup
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*


class ComparisonsModel(application: Application)
    : BaseViewModel(application) {

    companion object {
        const val STATUS_NONE = 0
        const val STATUS_DONE = 1
        const val STATUS_NO_INTERNET = 3
        const val STATUS_DOWNLOADING = 4
        const val STATUS_SERVER_ERROR = 5
        const val STATUS_CLIENT_ERROR = 6
        const val STATUS_NOT_ALLOWED = 7

        private const val REFRESH_COMPARISONS_FREQUENCY_IN_MILLIS = 12 * 60 * 60 * 1000L
    }

    private val mStatus = MutableLiveData<Int>().apply { STATUS_NONE }

    private val mAverages = MutableLiveData<List<ComparisonCacheData>>()

    private val mSelectedTerm = MutableLiveData<TermDao.TermShortInfo>()

    val averages get() = (mAverages).asImmutable

    val status get() = handleBackground(mStatus).asImmutable

    val selectedTermInfo get() = mSelectedTerm.asImmutable

    private var requestedTermId = 0

    private fun downloadNewComparisons() {
        try {
            val requestedTermId = this.requestedTermId
            if (!MobiregPreferences.get(context).allowedInstantNotifications) {
                mStatus.postValue(STATUS_NOT_ALLOWED)
                return
            }

            val connected = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.activeNetworkInfo?.isConnected == true

            if (!connected) {
                mStatus.postValue(STATUS_NO_INTERNET)
            } else {
                mStatus.postValue(STATUS_DOWNLOADING)
                val body = Jsoup
                        .connect(getLinkToServerWithParams(requestedTermId))
                        .ignoreContentType(true)
                        .execute()
                        .body()

                val jo = JsonParser()
                        .parse(body)!!
                        .asJsonObject!!

                if (jo["success"]?.asBoolean != true) {
                    mStatus.postValue(STATUS_CLIENT_ERROR)
                } else {
                    val comparisons = mutableListOf<ComparisonCacheData>()
                    jo["averages"]?.asJsonArray?.forEach {
                        comparisons.add(ComparisonCacheData(it!!.asJsonObject))
                    }

                    AppDatabase.getAppDatabase(context)
                            .comparisonDao.apply {
                        deleteAll()
                        insertComparisons(comparisons)
                    }
                    MobiregPreferences.get(context).apply {
                        lastComparisonsRefreshTime = System.currentTimeMillis()
                        downloadedComparisonsTermId = requestedTermId
                    }

                    mAverages.postValue(comparisons)
                    mStatus.postValue(STATUS_DONE)
                    postTermInfo()
                }

            }
        } catch (ste: SocketTimeoutException) {
            mStatus.postValue(STATUS_NO_INTERNET)
        } catch (uhe: UnknownHostException) {
            mStatus.postValue(STATUS_NO_INTERNET)
        } catch (e: Exception) {
            e.printStackTrace()
            mStatus.postValue(STATUS_SERVER_ERROR)
        }
    }

    private val shouldRefreshNow
        get() =
            MobiregPreferences.get(context).lastComparisonsRefreshTime + REFRESH_COMPARISONS_FREQUENCY_IN_MILLIS < System.currentTimeMillis()


    fun considerRefreshingData() {
        if (shouldRefreshNow) {
            if ((context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager?)
                            ?.activeNetworkInfo?.isConnected == true)
                refreshDataFromInternet()
        }
    }

    private var shouldNowDownloadData = false
    fun refreshDataFromInternet() {
        requestedTermId = MobiregPreferences.get(context).lastSelectedTerm
        shouldNowDownloadData = true
        cancelLastTask()
        handleBackground()
    }

    private fun postTermInfo() {
        val term = AppDatabase.getAppDatabase(context)
                .termDao.getTermShortInfo(requestedTermId)
        mSelectedTerm.postValue(if (term == null) TermDao.TermShortInfo(0, "Cały rok", "U")
        else TermDao.TermShortInfo(term.id, TermDao.getNiceTermName(term.type, term.name), term.type))
    }

    override fun doInBackground() {
        if (shouldNowDownloadData || shouldRefreshNow) {
            shouldNowDownloadData = false
            downloadNewComparisons()
        } else {
            val all = AppDatabase.getAppDatabase(context)
                    .comparisonDao.getAll()

            requestedTermId = MobiregPreferences.get(context).downloadedComparisonsTermId
            postTermInfo()
            mAverages.postValue(all)
        }
    }

    private fun getLinkToServerWithParams(termId: Int) = buildString {
        MobiregPreferences.get(context).apply {
            append(DedicatedServerManager(context).averagesLink ?: return@buildString)

            append("?l=")
            append(loginAndHostIfNeeded)

            append("&p=")
            append(password)

            append("&h=")
            append(host)

            append("&n=")
            append(name)

            append("&s=")
            append(surname)

            append("&t=")
            append(termId)

            append("&c=")
            append(Locale.getDefault()?.language ?: "_")

            append("&v=")
            append(BuildConfig.VERSION_CODE)
        }
    }
}