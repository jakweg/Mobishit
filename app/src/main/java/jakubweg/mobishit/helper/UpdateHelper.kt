package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import jakubweg.mobishit.db.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ChangedObjectsLog {
    fun new(action: String?, obj: Any) {
        when (action?.first() ?: return) {
            'I' -> addedEntities.add(obj)
            'U' -> modifiedEntities.add(obj)
            'D' -> deletedEntities.add(obj)
        }
    }

    var addedEntities = ArrayList<Any>()
    var modifiedEntities = ArrayList<Any>()
    var deletedEntities = ArrayList<Any>()
}

class UpdateHelper(private val context: Context) {

    private var mIsFirstTime = false
    var newMarks = mutableListOf<MarkData>()
    var newMessages = mutableListOf<MessageData>()
    var newAttendances = mutableListOf<AttendanceData>()
    var modifiedAttendances = mutableListOf<Pair<String, AttendanceData>>() // before <name> and now <*>
    var newEvents = mutableListOf<EventData>()
    var deletedMarks = mutableListOf<MarkDao.DeletedMarkData>()

    private var onNewMark: SimpleCallback<MarkData> = doNothingCallback()
    private var onNewMessage: SimpleCallback<MessageData> = doNothingCallback()
    private var onNewAttendance: SimpleCallback<AttendanceData> = doNothingCallback()
    private var onAttendanceModified: SimpleCallback<AttendanceData> = doNothingCallback()
    private var onNewEvents: SimpleCallback<EventData> = doNothingCallback()
    private var onDeleteMark: SimpleCallback<MarkData> = doNothingCallback()

    private val log = ChangedObjectsLog()

    val isFirstTime get() = mIsFirstTime

    val isAnythingNew
        get() = mIsFirstTime ||
                newMarks.isNotEmpty() ||
                newMessages.isNotEmpty() ||
                newAttendances.isNotEmpty() ||
                modifiedAttendances.isNotEmpty() ||
                newEvents.isNotEmpty() ||
                deletedMarks.isNotEmpty()


    fun doUpdate() {
        val preferences = MobiregPreferences.get(context)

        if (!preferences.isSignedIn)
            throw IllegalStateException("User is not signed in!")

        if (preferences.lmt == -1L)
            makeFirstTimeUpdate()
        else
            makeUpdateNotFirstTime()

        preferences.setLastRefreshTimeToNow()
    }

    @SuppressLint("ApplySharedPref")
    private fun makeFirstTimeUpdate() {
        val preferences = MobiregPreferences.get(context)

        val endDate = "2019-06-30"

        //ignore everything
        onNewMark = doNothingCallback()
        onNewMessage = doNothingCallback()
        onNewAttendance = doNothingCallback()
        onAttendanceModified = doNothingCallback()
        onNewEvents = doNothingCallback()
        onDeleteMark = doNothingCallback()


        mIsFirstTime = true

        makeConnection(buildString {
            append("login=eparent")
            append("&pass=eparent")
            append("&device_id="); append(preferences.deviceId)
            append("&app_version=60")
            append("&parent_login="); append(preferences.loginAndHostIfNeeded)
            append("&parent_pass="); append(preferences.password)
            append("&start_date="); append(preferences.startDate)
            append("&end_date="); append(endDate)
            append("&get_all_mark_groups="); append(preferences.getAllMarkGroups.to0or1())
            append("&student_id="); append(preferences.studentId)
        }, preferences.host!!, preferences.deviceId).use {
            parseUpdate(JsonReader(it), false)
        }

        insertNoEventSubject()
    }

    private fun insertNoEventSubject() {
        database.mainDao.insert(SubjectData(0, "Inne wydarzenia", "IW", false))
    }

    private val database by lazy { AppDatabase.getAppDatabase(context) }

    @SuppressLint("ApplySharedPref")
    private fun makeUpdateNotFirstTime() {

        val preferences = MobiregPreferences.get(context)

        val endDate = "2020-06-30"


        newMarks.clear()
        newMessages.clear()
        newAttendances.clear()
        newEvents.clear()
        deletedMarks.clear()

        onNewMessage = makeCallback { newMessages.add(it) }
        onNewMark = makeCallback { newMarks.add(it) }
        onNewAttendance = makeCallback { newAttendances.add(it) }
        onAttendanceModified = makeCallback {
            newAttendances.removeFirstIf { e -> e.id == it.id }
            modifiedAttendances.add(Pair(database.attendanceDao.getAttendanceType(it.id)
                    ?: return@makeCallback, it))
        }
        onNewEvents = makeCallback { data ->
            if (data.status == EventDao.STATUS_CANCELED || (data.status == EventDao.STATUS_SCHEDULED &&
                            data.substitution.let { it == EventDao.SUBSTITUTION_NEW_LESSON || it == EventDao.SUBSTITUTION_OLD_LESSON })) {
                newEvents.removeFirstIf { it.id == data.id }
                newEvents.add(data)
            }
        }
        onDeleteMark = makeCallback {
            newMarks.removeFirstIf { e -> e.id == it.id }
            deletedMarks.add(database.markDao.getDeletedMarkInfo(it.id))
        }

        makeConnection(
                inputToWrite = buildString {
                    append("login=eparent")
                    append("&pass=eparent")
                    append("&device_id="); append(preferences.deviceId)
                    append("&app_version=60")
                    append("&parent_login="); append(preferences.loginAndHostIfNeeded)
                    append("&parent_pass="); append(preferences.password)
                    append("&start_date="); append(preferences.startDate)
                    append("&end_date="); append(endDate)
                    append("&last_end_date="); append(endDate)
                    append("&lmt="); append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .also {
                            it.timeZone = TimeZone.getTimeZone("CET")
                        }.format(Date(preferences.lmt)))
                    append("&get_all_mark_groups="); append(preferences.getAllMarkGroups.to0or1())
                    append("&student_id="); append(preferences.studentId)
                }, host = preferences.host!!, deviceId = preferences.deviceId)
                .use {
                    parseUpdate(JsonReader(it), false)
                }
    }

    @SuppressLint("ApplySharedPref")
    private fun parseUpdate(reader: JsonReader, ignoreLmt: Boolean) {
        var millisOfRequest = 0L
        reader.apply {
            kotlin.run {
                beginObject()

                while (hasNext()) {
                    val name = nextName()!!

                    if (peek() != JsonToken.BEGIN_ARRAY) {
                        handleErrorJson(name, reader)
                        return@run
                    }
                    beginArray()
                    when (name) {
                        "Teachers" -> forEachInArray { DataCreator.teacher(it, log) }
                        "Rooms" -> forEachInArray { DataCreator.roomData(it, log) }
                        "Terms" -> forEachInArray { DataCreator.termData(it, log) }
                        "Subjects" -> forEachInArray { DataCreator.subjectData(it, log) }
                        "Groups" -> forEachInArray { DataCreator.groupData(it, log) }
                        "GroupTerms" -> forEachInArray { DataCreator.groupTerm(it, log) }
                        "MarkScaleGroups" -> forEachInArray { DataCreator.markScaleGroup(it, log) }
                        "MarkScales" -> forEachInArray { DataCreator.markScale(it, log) }
                        "MarkDivisionGroups" -> forEachInArray { DataCreator.markDivisionGroup(it, log) }
                        "MarkKinds" -> forEachInArray { DataCreator.markKind(it, log) }
                        "MarkGroupGroups" -> forEachInArray { DataCreator.markGroupGroup(it, log) }
                        "MarkGroups" -> forEachInArray { DataCreator.markGroup(it, log) }
                        "EventTypes" -> forEachInArray { DataCreator.eventType(it, log) }
                        "EventTypeTeachers" -> forEachInArray { DataCreator.eventTypeTeacher(it, log) }
                        "EventTypeTerms" -> forEachInArray { DataCreator.eventTypeTerm(it, log) }
                        "EventTypeGroups" -> forEachInArray { DataCreator.eventTypeGroup(it, log) }
                        "Events" -> forEachInArray { DataCreator.eventData(it, log) }
                        "EventIssues" -> forEachInArray { DataCreator.eventIssue(it, log) }
                        "EventEvents" -> forEachInArray { DataCreator.eventEvent(it, log) }
                        "AttendanceTypes" -> forEachInArray { DataCreator.attendanceType(it, log) }
                        "Attendances" -> forEachInArray { DataCreator.attendanceData(it, log) }
                        "Marks" -> forEachInArray { DataCreator.markData(it, log) }
                        "UserReprimands" -> forEachInArray { DataCreator.userReprimand(it, log) }
                        "StudentGroups" -> forEachInArray { DataCreator.studentGroup(it, log) }
                        "EventTypeSchedules" -> forEachInArray { DataCreator.eventTypeSchedule(it, log) }
                        "Lessons" -> forEachInArray { DataCreator.lessonData(it, log) }
                        "Messages" -> forEachInArray { DataCreator.messageData(it, log) }
                        "Settings" -> {
                            beginObject()
                            while (nextName() != "time")
                                skipValue()

                            val time = nextString()

                            if (!ignoreLmt) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("GMT")
                                millisOfRequest = sdf.parse(time).time

                            }
                            while (peek() != JsonToken.END_OBJECT) {
                                nextName(); skipValue()
                            }
                            endObject()
                        }
                        "Students", "GroupEducators", "EventSubjects", "MarkGroupIssues", "LessonsGroups", "PermissionGroups", "Permissions", "Averages" -> skipArrayContent()
                        else -> skipValue()
                    }
                    endArray()
                }
                endObject()

            }

        }


        if (millisOfRequest > 0)
            MobiregPreferences.get(context)
                    .setLmt(millisOfRequest)
        commitChanges()
    }

    private fun commitChanges() {
        val dao = database.mainDao

        for (element in log.addedEntities) {
            when (element) {
                is MarkData -> onNewMark.call(element)
                is MessageData -> onNewMessage.call(element)
                is AttendanceData -> onNewAttendance.call(element)
                is EventData -> onNewEvents.call(element)
            }
            dao.insertAny(element)
        }

        for (element in log.modifiedEntities) {
            when (element) {
                is MarkData -> onNewMark.call(element)
                is AttendanceData -> onAttendanceModified.call(element)
                is EventData -> onNewEvents.call(element)
            }
            dao.insertAny(element)
        }

        for (element in log.deletedEntities) {
            when (element) {
                is MarkData -> onDeleteMark.call(element)
                is AttendanceData -> {
                    newAttendances.removeFirstIf { it.id == element.id }
                    modifiedAttendances.removeFirstIf { it.second.id == element.id }
                }
            }
            dao.deleteAny(element)
        }
    }

    private fun handleErrorJson(name: String, jsonReader: JsonReader) {
        jsonReader.apply {
            if (name == "errno") {
                val errno = nextInt()
                if (errno == 102)
                    throw InvalidPasswordException()
                else
                    throw IllegalArgumentException("Can't handle errno $errno")
            } else
                throw IllegalArgumentException("Excepted \"errno\", got $name")
        }
    }

    companion object {
        fun makeConnection(inputToWrite: String,
                           host: String,
                           deviceId: Int
        ): Reader {
            val url = URL("https://www.mobireg.pl/$host/modules/api/njson.php")

            return run {
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    setRequestProperty("User-Agent", "Andreg $deviceId")
                }

                val os = connection.outputStream

                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(inputToWrite)
                writer.flush()
                writer.close()
                os.close()

                InputStreamReader(BufferedInputStream(connection.inputStream), Charsets.UTF_8)
            }
        }
    }


    private fun JsonReader.skipArrayContent() {
        while (this.peek() != JsonToken.END_ARRAY) {
            when (this.peek()) {
                JsonToken.BEGIN_ARRAY -> {
                    this.beginArray()
                    this.skipArrayContent()
                    this.endArray()
                }
                //JsonToken.END_ARRAY -> return
                else -> this.skipValue()
            }
        }
    }
}

private inline fun JsonReader.forEachInArray(function: (JsonReader) -> Any) {
    while (hasNext()) {
        try {
            function.invoke(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        while (peek() != JsonToken.END_OBJECT)
            skipValue()
        endObject()
    }
}

private fun Boolean.to0or1() = if (this) "1" else "0"

class InvalidPasswordException : IllegalStateException("Can't log in, probably password has changed")


private inline fun <E> MutableCollection<E>.removeFirstIf(test: (E) -> Boolean) {
    val it = iterator()
    while (it.hasNext()) if (test.invoke(it.next())) {
        it.remove()
        return
    }
}


interface SimpleCallback<T> {
    fun call(obj: T)
}

interface SimpleCallback2<T1, T2> {
    fun call(obj1: T1, obj2: T2)
}

inline fun <T> makeCallback(crossinline function: (T) -> Unit) = object : SimpleCallback<T> {
    override fun call(obj: T) = function(obj)
}

inline fun <T1, T2> makeCallback2(crossinline function: (T1, T2) -> Unit) = object : SimpleCallback2<T1, T2> {
    override fun call(obj1: T1, obj2: T2) = function(obj1, obj2)
}

fun <T> doNothingCallback() = object : SimpleCallback<T> {
    override fun call(obj: T) {}
}