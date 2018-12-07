package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Query

@Dao
interface EventDao {
    companion object {
        const val STATUS_SCHEDULED = 0
        const val STATUS_COMPLETED = 1
        const val STATUS_CANCELED = 2

        const val SUBSTITUTION_NONE = 0
        const val SUBSTITUTION_OLD_LESSON = 1
        const val SUBSTITUTION_NEW_LESSON = 2
    }

    @Query("SELECT date FROM Events GROUP BY date ORDER BY date ASC")
    fun getDaysWithEventsAsMillis(): List<Long>


    class EventLongInfo(val subjectName: String?, val description: String?, val status: Int, val teacherName: String?, val number: Int?, val startTime: String, val endTime: String, val substitution: Int, val attendanceName: String?, val color: Int?, val roomName: String?)

    @Query("""SELECT Subjects.name AS subjectName, Events.name AS description, Events.status, Teachers.name || ' ' || Teachers.surname AS teacherName,
        number, startTime, endTime, substitution, AttendanceTypes.name AS attendanceName, color, Rooms.name AS roomName
FROM Events
LEFT JOIN Attendances ON Attendances.eventId = Events.id
LEFT JOIN AttendanceTypes ON AttendanceTypes.id = Attendances.typeId
INNER JOIN EventTypes ON EventTypes.id = Events.eventTypeId
LEFT JOIN Rooms On Rooms.id = Events.roomId
LEFT JOIN Subjects ON Subjects.id = EventTypes.subjectId
LEFT JOIN EventTypeTeachers ON EventTypeTeachers.eventTypeId = EventTypes.id
LEFT JOIN Teachers ON Teachers.id = EventTypeTeachers.teacherId
WHERE Events.date = :day
ORDER BY startTime, number, substitution""")
    fun getAllEventsByDay(day: Long): List<EventLongInfo>


    class EventShortInfo(val subjectName: String?, val number: Int?, val time: String, val roomName: String?)

    @Query("""SELECT IFNULL(Subjects.name, Events.name) AS subjectName, number,
        startTime || '|'|| endTime AS time, Rooms.name AS roomName
FROM Events
INNER JOIN EventTypes ON EventTypes.id = Events.eventTypeId
LEFT JOIN Rooms On Rooms.id = Events.roomId
LEFT JOIN Subjects ON Subjects.id = EventTypes.subjectId
WHERE Events.date = :day AND status != 2 AND substitution != 1
ORDER BY startTime, number""")// not cancelled nor replaced
    fun getShortEventsInfoByDay(day: Long): List<EventShortInfo>


    class AttendanceShortInfo(val subjectName: String?, val date: Long, val number: Int?, val startTime: String?, val attendanceName: String, val isAbsent: Boolean)

    @Query("""SELECT IFNULL(Subjects.name, Events.name) AS subjectName, date, Events.number, Events.startTime,
        AttendanceTypes.name AS attendanceName, countAs = 'A' as isAbsent FROM Attendances
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN AttendanceTypes ON AttendanceTypes.id = Attendances.typeId
LEFT JOIN EventTypes ON Events.eventTypeId = EventTypes.id
LEFT JOIN Subjects ON Subjects.id = EventTypes.subjectId
WHERE (countAs != 'P') + :allowPresentAttendance > 0 AND Attendances.id IN (:ids)
ORDER BY startTime, number""")
    fun getAttendanceInfoByIds(ids: List<Int>, allowPresentAttendance: Boolean): List<AttendanceShortInfo>


    @Query("SELECT Count(id) > 0 FROM Events WHERE Events.date = :date LIMIT 1")
    fun hasEventsForDay(date: Long): Boolean


    class CountdownServiceLesson(
            var name: String?,
            var number: Int?,
            val startTime: String, val endTime: String, val roomName: String?) {
        companion object {
            @JvmStatic
            private fun getSecondsOfDay(hourAndMinute: String): Int {
                val hours: Int
                val minutes: Int
                val seconds: Int
                when (hourAndMinute.length) {
                    5 -> {
                        hours = hourAndMinute.substring(0, 2).toInt()
                        minutes = hourAndMinute.substring(3, 5).toInt()
                        seconds = 0
                    }
                    8 -> {
                        hours = hourAndMinute.substring(0, 2).toInt()
                        minutes = hourAndMinute.substring(3, 5).toInt()
                        seconds = hourAndMinute.substring(6, 8).toInt()
                    }
                    else -> throw IllegalArgumentException("Can't get second of day! Argument: $hourAndMinute")
                }
                return (hours * 60 * 60 + minutes * 60 + seconds)
            }
        }

        @Ignore
        val startSeconds = getSecondsOfDay((startTime))
        @Ignore
        val endSeconds = getSecondsOfDay((endTime))
    }

    @Query("""SELECT IFNULL(Subjects.name, Events.name) as name, Events.number, Events.startTime, Events.endTime, Rooms.name as roomName FROM Events
LEFT JOIN EventTypes ON Events.eventTypeId = EventTypes.id
LEFT JOIN Subjects ON Subjects.id = EventTypes.subjectId
LEFT JOIN Rooms ON Rooms.id = Events.roomId
WHERE date = :day AND status != 2 AND substitution != 1
GROUP BY startTime ORDER BY startTime, number""") // not cancelled nor replaced
    fun getCountdownServiceLessons(day: Long): List<CountdownServiceLesson>


    @Query("SELECT startTime FROM Events WHERE status != 2 AND substitution != 1 AND date = :date ORDER BY number ASC LIMIT 1")
    fun getFirstLessonStartTime(date: Long): String?

    @Query("SELECT endTime FROM Events WHERE status != 2 AND substitution != 1 AND date = :date ORDER BY number DESC LIMIT 1")
    fun getLastLessonEndTime(date: Long): String?


    @Query("SELECT Subjects.name FROM EventTypes INNER JOIN Subjects ON Subjects.id = EventTypes.subjectId WHERE EventTypes.id = :eventTypeId")
    fun getSubjectNameByEventType(eventTypeId: Int): String?

    @Query("""SELECT Teachers.name || ' ' || Teachers.surname FROM EventTypes
INNER JOIN EventTypeTeachers ON EventTypeTeachers.eventTypeId = EventTypes.id
INNER JOIN Teachers ON Teachers.id = EventTypeTeachers.teacherId
WHERE EventTypes.id = :eventTypeId""")
    fun getTeacherFullNameByEventType(eventTypeId: Int): String?

}
