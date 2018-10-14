package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import jakubweg.mobishit.db.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class UpdateHelper(private val context: Context) {

    var isFirstTime = false
    var newMarks = mutableListOf<MarkData>()
    var newMessages = mutableListOf<MessageData>()
    var newAttendances = mutableListOf<AttendanceData>()
    var newEvents = mutableListOf<EventData>()


    private var onNewMark: (MarkData) -> Unit = { }
    private var onNewMessage: (MessageData) -> Unit = { }
    private var onNewAttendance: (AttendanceData) -> Unit = { }
    private var onNewEvents: (EventData) -> Unit = { }


    private var saveEverySyncEnabled = false

    fun doUpdate() {
        val preferences = MobiregPreferences.get(context)

        if (!preferences.isSignedIn)
            throw IllegalStateException("User is not signed in!")
        saveEverySyncEnabled = preferences.logEverySync

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
        onNewMark = { }
        onNewMessage = { }
        onNewAttendance = { }
        onNewEvents = { }


        isFirstTime = true

        makeLoggedConnection(buildString {
            append("login=eparent")
            append("&pass=eparent")
            append("&device_id="); append(preferences.deviceId)
            append("&app_version=60")
            append("&parent_login="); append(preferences.login!!)
            append('.'); append(preferences.host!!)
            append("&parent_pass="); append(preferences.password)
            append("&start_date="); append(preferences.startDate)
            append("&end_date="); append(endDate)
            append("&get_all_mark_groups="); append(preferences.getAllMarkGroups.to0or1())
            append("&student_id="); append(preferences.studentId)
        }, preferences.host!!, preferences.deviceId, saveEverySyncEnabled, context.applicationInfo.dataDir).use {
            parseUpdate(JsonReader(it))
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun makeUpdateNotFirstTime() {

        val preferences = MobiregPreferences.get(context)

        val endDate = "2019-06-30"


        newMarks.clear()
        newMessages.clear()
        newAttendances.clear()
        newEvents.clear()

        onNewMessage = { newMessages.add(it) }
        onNewMark = { newMarks.add(it) }
        onNewAttendance = { newAttendances.add(it) }
        onNewEvents = { data ->
            if (data.status == EventDao.STATUS_CANCELED ||
                    (data.status == EventDao.STATUS_SCHEDULED &&
                            data.substitution.let { it == EventDao.SUBSTITUTION_NEW_LESSON || it == EventDao.SUBSTITUTION_OLD_LESSON }))
                newEvents.add(data)
        }

        makeLoggedConnection(
                inputToWrite = buildString {
                    append("login=eparent")
                    append("&pass=eparent")
                    append("&device_id="); append(preferences.deviceId)
                    append("&app_version=60")
                    append("&parent_login="); append(preferences.login)
                    append('.'); append(preferences.host)
                    append("&parent_pass="); append(preferences.password)
                    append("&start_date="); append(preferences.startDate)
                    append("&end_date="); append(endDate)
                    append("&last_end_date="); append(endDate)
                    append("&lmt="); append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(preferences.lmt)))
                    append("&get_all_mark_groups="); append(preferences.getAllMarkGroups.to0or1())
                    append("&student_id="); append(preferences.studentId)
                }, host = preferences.host!!, deviceId = preferences.deviceId,
                logEverythingEnabled = saveEverySyncEnabled, dataDir = context.applicationInfo.dataDir)
                .use {
                    parseUpdate(JsonReader(it))
                }
    }

    @SuppressLint("ApplySharedPref")
    private fun parseUpdate(reader: JsonReader) {
        reader.apply {
            var millisOfRequest = 0L
            val db = AppDatabase.getAppDatabase(context)
            db.runInTransaction {
                beginObject()


                val dao = db.mainDao
                while (hasNext()) {
                    val name = nextName()!!

                    if (peek() != JsonToken.BEGIN_ARRAY) {
                        handleErrorJson(name, reader)
                        return@runInTransaction
                    }
                    beginArray()
                    when (name) {
                        "Teachers" -> forEachInArray { insertElementToDao(dao, DataCreator.teacher(it)) }
                        "Rooms" -> forEachInArray { insertElementToDao(dao, DataCreator.roomData(it)) }
                        "Terms" -> forEachInArray { insertElementToDao(dao, DataCreator.termData(it)) }
                        "Subjects" -> forEachInArray { insertElementToDao(dao, DataCreator.subjectData(it)) }
                        "Groups" -> forEachInArray { insertElementToDao(dao, DataCreator.groupData(it)) }
                        "GroupTerms" -> forEachInArray { insertElementToDao(dao, DataCreator.groupTerm(it)) }
                        "MarkScaleGroups" -> forEachInArray { insertElementToDao(dao, DataCreator.markScaleGroup(it)) }
                        "MarkScales" -> forEachInArray { insertElementToDao(dao, DataCreator.markScale(it)) }
                        "MarkDivisionGroups" -> forEachInArray { insertElementToDao(dao, DataCreator.markDivisionGroup(it)) }
                        "MarkKinds" -> forEachInArray { insertElementToDao(dao, DataCreator.markKind(it)) }
                        "MarkGroupGroups" -> forEachInArray { insertElementToDao(dao, DataCreator.markGroupGroup(it)) }
                        "MarkGroups" -> forEachInArray { insertElementToDao(dao, DataCreator.markGroup(it)) }
                        "EventTypes" -> forEachInArray { insertElementToDao(dao, DataCreator.eventType(it)) }
                        "EventTypeTeachers" -> forEachInArray { insertElementToDao(dao, DataCreator.eventTypeTeacher(it)) }
                        "EventTypeTerms" -> forEachInArray { insertElementToDao(dao, DataCreator.eventTypeTerm(it)) }
                        "EventTypeGroups" -> forEachInArray { insertElementToDao(dao, DataCreator.eventTypeGroup(it)) }
                        "Events" -> forEachInArray { insertElementToDao(dao, DataCreator.eventData(it)) }
                        "EventIssues" -> forEachInArray { insertElementToDao(dao, DataCreator.eventIssue(it)) }
                        "EventEvents" -> forEachInArray { insertElementToDao(dao, DataCreator.eventEvent(it)) }
                        "AttendanceTypes" -> forEachInArray { insertElementToDao(dao, DataCreator.attendanceType(it)) }
                        "Attendances" -> forEachInArray { insertElementToDao(dao, DataCreator.attendanceData(it)) }
                        "Marks" -> forEachInArray { insertElementToDao(dao, DataCreator.markData(it)) }
                        "UserReprimands" -> forEachInArray { insertElementToDao(dao, DataCreator.userReprimand(it)) }
                        "StudentGroups" -> forEachInArray { insertElementToDao(dao, DataCreator.studentGroup(it)) }
                        "EventTypeSchedules" -> forEachInArray { insertElementToDao(dao, DataCreator.eventTypeSchedule(it)) }
                        "Lessons" -> forEachInArray { insertElementToDao(dao, DataCreator.lessonData(it)) }
                        "Messages" -> forEachInArray { insertElementToDao(dao, DataCreator.messageData(it)) }
                        "Settings" -> {
                            beginObject()
                            while (nextName() != "time")
                                skipValue()

                            val time = nextString()

                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                            sdf.timeZone = TimeZone.getTimeZone("GMT")
                            millisOfRequest = sdf.parse(time).time


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

                if (millisOfRequest > 0)
                    MobiregPreferences.get(context)
                            .setLmt(millisOfRequest)
            }
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

    private fun insertElementToDao(dao: MainDao, element: Any) {
        when (element) {
            is MarkData -> {
                dao.insert(element); onNewMark.invoke(element)
            }
            is MessageData -> {
                dao.insert(element); onNewMessage.invoke(element)
            }
            is AttendanceData -> {
                dao.insert(element); onNewAttendance.invoke(element)
            }
            is EventData -> {
                dao.insert(element); onNewEvents.invoke(element)
            }
            else -> dao.insertAny(element)
        }
    }


    companion object {
        @JvmStatic
        fun makeLoggedConnection(inputToWrite: String,
                                 host: String,
                                 deviceId: Int,
                                 logEverythingEnabled: Boolean,
                                 dataDir: String
        ): Reader {
            val url = URL("https://www.mobireg.pl/$host/modules/api/njson.php")

            return if (logEverythingEnabled) {
                File(dataDir, "syncs").run {
                    mkdirs()
                    val date = DateHelper.millisToStringTime(Calendar.getInstance()!!.timeInMillis)
                    File(this, date).writer(Charsets.UTF_8).use { logger ->
                        logger.write("Beginning of connection to ")
                        logger.write(url.toString())
                        logger.write("\ndate: ")
                        logger.write(date)
                        logger.write("\n\n")


                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 5000
                            readTimeout = 20 * 1000
                            requestMethod = "POST"
                            doInput = true
                            doOutput = true
                            setRequestProperty("User-Agent", "Andreg $deviceId")
                        }

                        logger.write("Connecting... ")

                        val os = connection.outputStream

                        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                        writer.write(inputToWrite)
                        writer.flush()
                        writer.close()

                        logger.write("result: ${connection.responseMessage}, code: ${connection.responseCode}\n\n")

                        logger.write("INPUT:\n")
                        logger.write(inputToWrite)
                        logger.write("\nEND OF INPUT\n\n")

                        logger.write("OUTPUT:\n")

                        val inputAsString = try {
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

                            os.close()

                            String(buf.toByteArray(), Charsets.UTF_8)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            logger.write("Error while downloading content!")
                            e.printStackTrace(PrintWriter(logger))

                            logger.close()
                            throw e
                        }


                        logger.write(inputAsString)

                        logger.write("\nEND OF OUTPUT\n")

                        logger.flush()

                        StringReader(inputAsString)
                    }
                }
            } else {
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

    private inline fun JsonReader.forEachInArray(function: (JsonReader) -> Any) {
        while (hasNext()) function.invoke(this)
    }

    private fun Boolean.to0or1() = if (this) "1" else "0"

    class InvalidPasswordException : IllegalStateException("Can't log in, probably password has changed")
}

