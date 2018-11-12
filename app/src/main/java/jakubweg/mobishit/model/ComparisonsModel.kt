package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jakubweg.mobishit.db.asStringOrNull
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregPreferences
import org.jsoup.Jsoup
import java.net.SocketTimeoutException


class ComparisonsModel(application: Application)
    : BaseViewModel(application) {

    class SubjectInfo(val subjectName: String,
                      val averageStudent: String, val averageClass: String, val averageSchool: String,
                      val positionInClass: String?, val maxPositionInClass: String?,
                      val positionInSchool: String?, val maxPositionInSchool: String?,
                      val classImg: String?, val schoolImg: String?
    ) {
        constructor(jo: JsonObject) : this(
                jo["subject"]!!.asString!!,
                jo["avg_person"]!!.asString!!,
                jo["avg_class"]!!.asString!!,
                jo["avg_school"]!!.asString!!,
                jo["pos_class"].asStringOrNull,
                jo["max_class"].asStringOrNull,
                jo["pos_school"].asStringOrNull,
                jo["max_school"].asStringOrNull,
                jo["img_class"].asStringOrNull,
                jo["img_school"].asStringOrNull
        )
    }

    companion object {
        const val STATUS_NONE = 0
        const val STATUS_DONE = 1
        const val STATUS_NO_INTERNET = 3
        const val STATUS_DOWNLOADING = 4
        const val STATUS_SERVER_ERROR = 5
        const val STATUS_CLIENT_ERROR = 6
        const val STATUS_NOT_ALLOWED = 7
    }

    private val mStatus = MutableLiveData<Int>().apply { STATUS_NONE }

    private val mAverages = MutableLiveData<List<SubjectInfo>>()

    val averages get() = handleBackground(mAverages).asImmutable

    val status get() = handleBackground(mStatus).asImmutable

    fun tryAgain() {
        cancelLastTask()
        handleBackground()
    }

    override fun doInBackground() {
        try {
            if (!MobiregPreferences.get(context).allowedInstantNotifications) {
                mStatus.postValue(STATUS_NOT_ALLOWED)
                return
            }

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (!connectivityManager.activeNetworkInfo.isConnected) {
                mStatus.postValue(STATUS_NO_INTERNET)
            } else {
                mStatus.postValue(STATUS_DOWNLOADING)
                val body = Jsoup
                        .connect(linkToServerWithParams)
                        .ignoreContentType(true)
                        .execute()
                        .body()

                val jo = JsonParser()
                        .parse(body)!!
                        .asJsonObject!!

                if (jo["success"]?.asBoolean != true) {
                    mStatus.postValue(STATUS_CLIENT_ERROR)
                } else {
                    val averages = mutableListOf<SubjectInfo>()
                    jo["averages"]?.asJsonArray?.forEach {
                        averages.add(SubjectInfo(it!!.asJsonObject))
                    }
                    mAverages.postValue(averages)
                    mStatus.postValue(STATUS_DONE)
                }

            }
        } catch (ste: SocketTimeoutException) {
            mStatus.postValue(STATUS_NO_INTERNET)
        } catch (e: Exception) {
            e.printStackTrace()
            mStatus.postValue(STATUS_SERVER_ERROR)
        }
    }

    private val linkToServerWithParams
        get() = buildString {
            MobiregPreferences.get(context).apply {
                append(DedicatedServerManager(context).averagesLink)

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
            }
        }
}