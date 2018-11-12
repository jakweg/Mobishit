package jakubweg.mobishit.model

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.NotificationHelper
import jakubweg.mobishit.helper.UpdateHelper
import java.io.Reader

class LoginDataModel(application: Application)
    : BaseViewModel(application) {

    companion object {
        const val STATUS_NOT_WORKING = 0
        const val STATUS_WORKING = 1
        const val STATUS_FAILED = 3
        const val STATUS_FAILED_WRONG_INPUTS = 4
        const val STATUS_CHOOSE_STUDENT = 5
        const val STATUS_SUCCESS = 6
    }

    class UserData(val studentId: Int, val login: String, val host: String,
                   val pass: String, val hasHostInLogin: Boolean, val name: String,
                   val surname: String, val sex: String, val phone: String)

    private val mStatus = MutableLiveData<Int>()
            .apply { value = STATUS_NOT_WORKING }


    val status get() = mStatus.asImmutable

    var users = mutableListOf<UserData>()

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

    fun setStatusSuccess() {
        mStatus.value = STATUS_SUCCESS
    }

    fun performLogin() {
        setStatusSuccess()
    }

    @SuppressLint("ApplySharedPref")
    override fun doInBackground() {
        try {
            val result = tryLoginWithHost()
            when (result) {
                STATUS_SUCCESS, STATUS_CHOOSE_STUDENT -> mStatus.postValue(result)
                STATUS_FAILED_WRONG_INPUTS -> mStatus.postValue(tryLoginWithoutHost())
                else -> mStatus.postValue(STATUS_FAILED)
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


    private fun tryLoginWithHost(): Int {
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
                    append("."); append(host)
                    append("&parent_pass="); append(password)
                    append("&view=ParentStudents")
                },
                host = host,
                deviceId = deviceId,
                logEverythingEnabled = isLoggingEnabled,
                dataDir = context.applicationInfo.dataDir
        ).use { reader ->
            return processLoginRequestOutput(reader, true)
        }
    }

    private fun tryLoginWithoutHost(): Int {
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
                    append("&parent_pass="); append(password)
                    append("&view=ParentStudents")
                },
                host = host,
                deviceId = deviceId,
                logEverythingEnabled = isLoggingEnabled,
                dataDir = context.applicationInfo.dataDir
        ).use { reader ->
            return processLoginRequestOutput(reader, false)
        }
    }

    private fun processLoginRequestOutput(reader: Reader, isWithHost: Boolean): Int {
        JsonParser().parse(JsonReader(reader)).asJsonObject.also { jo ->
            val users = mutableListOf<UserData>()

            when {
                jo.has("errno") ->
                    return (if (jo.get("errno")!!.asInt == 106)
                        STATUS_FAILED_WRONG_INPUTS else STATUS_FAILED)

                jo.has("ParentStudents") -> {
                    jo.get("ParentStudents").asJsonArray.also { arr ->
                        arr.forEach {
                            it.asJsonObject!!.apply {
                                val id = get("id")!!.asInt
                                val name = get("name")!!.asString
                                val surname = get("surname")!!.asString
                                val phone = get("phone")!!.asStringOrNull ?: ""
                                val sex = validateSex(get("sex")!!.asStringOrNull)

                                /*MobiregPreferences.get(context).also {
                                    it.setUserData(id, name, surname, phone, sex, loginName, host, isWithHost, password)} */
                                users.add(UserData(id, loginName, host, password, isWithHost, name, surname, sex, phone))
                            }
                        }

                        this.users = users
                        return STATUS_CHOOSE_STUDENT
                    }
                }
                else -> throw IllegalStateException("Can't log in - no errno nor ParentStudents")
            }
        }
        return STATUS_FAILED
    }


    private val JsonElement.asStringOrNull: String?
        get() = if (isJsonNull) null else asString

    private fun validateSex(sex: String?)
            : String = if (sex == "M" || sex == "K") sex else ""

}

