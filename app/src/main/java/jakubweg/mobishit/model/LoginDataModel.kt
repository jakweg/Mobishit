package jakubweg.mobishit.model

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.support.v4.app.NotificationCompat
import android.util.Log
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.NotificationHelper
import jakubweg.mobishit.helper.TimetableWidgetProvider
import jakubweg.mobishit.helper.UpdateHelper
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader

class LoginDataModel(application: Application)
    : BaseViewModel(application) {

    companion object {
        const val STATUS_NOT_WORKING = 0
        const val STATUS_WORKING = 1
        const val STATUS_FAILED = 3
        const val STATUS_FAILED_WRONG_INPUTS = 4
        const val STATUS_SUCCESS = 5

    }

    private val mStatus = MutableLiveData<Int>()
            .apply { value = STATUS_NOT_WORKING }


    val status get() = mStatus.asImmutable

    private var loginName = ""
    private var host = ""
    private var password = ""
    fun login(name: String, host_: String, pass: String) {
        cancelLastTask()
        mStatus.value = STATUS_WORKING
        loginName = name
        host = host_
        password = pass
        handleBackground()
    }

    fun performLogin() {
        mStatus.value = STATUS_SUCCESS
    }

    @SuppressLint("ApplySharedPref")
    override fun doInBackground() {
        try {
            val prefs = MobiregPreferences.get(context)
            val deviceId = prefs.deviceId

            val isLoggingEnabled = prefs.logEverySync

            UpdateHelper.makeLoggedConnection(
                    inputToWrite = buildString {
                        append("login=eparent")
                        append("&pass=eparent")
                        append("&device_id="); append(deviceId)
                        append("&app_version=60")
                        append("&parent_login="); append(loginName)
                        append('.'); append(host)
                        append("&parent_pass="); append(password)
                        append("&view=ParentStudents")
                    },
                    host = host,
                    deviceId = deviceId,
                    logEverythingEnabled = isLoggingEnabled,
                    dataDir = context.applicationInfo.dataDir
            ).use { reader ->
                JsonParser().parse(JsonReader(reader)).asJsonObject.also { jo ->
                    when {
                        jo.has("errno") ->
                            mStatus.postValue(if (jo.get("errno")!!.asInt == 106)
                                STATUS_FAILED_WRONG_INPUTS else STATUS_FAILED)

                        jo.has("ParentStudents") -> {
                            jo.get("ParentStudents").asJsonArray.also { arr ->
                                if (arr.size() != 1)
                                    throw IllegalStateException("ParentStudents array size is not 1")
                                arr[0]!!.asJsonObject!!.apply {
                                    val id = get("id")!!.asInt
                                    val name = get("name")!!.asString
                                    val surname = get("surname")!!.asString
                                     val phone = get("phone")!!.asStringOrNull ?: ""
                                    val sex = validateSex(get("sex")!!.asStringOrNull)

                                    MobiregPreferences.get(context).also {
                                        it.setUserData(id, name, surname, phone, sex, loginName, host, password)
                                    }
                                }
                                TimetableWidgetProvider.requestInstantUpdate(context)
                                mStatus.postValue(STATUS_SUCCESS)
                            }
                        }
                        else -> throw IllegalStateException("Can't log in - no errno nor ParentStudents")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("LoginDataModel", "Error while logging in", e)
            NotificationHelper(context).apply {
                createNotificationChannels()
                postNotification(NotificationCompat.Builder(context, NotificationHelper.CHANNEL_APP_STATE)
                        .setSmallIcon(R.drawable.ic_error)
                        .setContentTitle("Wystąpił błąd")
                        .setContentText("Rozwij po szczegóły")
                        .setStyle(NotificationCompat.BigTextStyle()
                                .bigText(e.toString())))
            }
            mStatus.postValue(STATUS_FAILED)
        }
    }


    private val JsonElement.asStringOrNull: String?
        get() = if (isJsonNull) null else asString

    private fun validateSex(sex: String?)
            : String = if (sex == "M" || sex == "K") sex else ""

}

