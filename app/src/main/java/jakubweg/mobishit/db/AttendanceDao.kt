package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Query

@Dao
interface AttendanceDao {

    class AttendanceCountInfo(val countAs: String, val count: Int)

    class AttendanceCountInfoHolder(
            val name: String,
            val start: Long,
            val end: Long,
            val counts: List<AttendanceCountInfo>) {

        val totalRecords get() = counts.sumBy { it.count }
    }


    @Query("SELECT MIN(date) FROM Attendances INNER JOIN Events ON Events.id = eventId")
    fun getFirstAttendanceDay(): Long?


    @Query("SELECT MAX(date) FROM Attendances INNER JOIN Events ON Events.id = eventId")
    fun getLastAttendanceDay(): Long?


    @Query("""SELECT countAs, count(Attendances.id) AS count FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                WHERE date BETWEEN :start AND :end GROUP BY countAs""")
    fun getAttendancesBetweenDates(start: Long, end: Long): List<AttendanceCountInfo>


    class AttendanceTypeInfo(val id: Int, val name: String, val count: Int, val color: Int)

    @Query("""SELECT AttendanceTypes.id, AttendanceTypes.name, count(Attendances.id) AS count, color FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                WHERE date BETWEEN :start AND :end GROUP BY typeId""")
    fun getDetailedAttendancesBetweenDates(start: Long, end: Long): List<AttendanceTypeInfo>

    @Query("""SELECT AttendanceTypes.id, AttendanceTypes.name, count(Attendances.id) AS count, color FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                INNER JOIN EventTypes ON EventTypes.id = eventTypeId
                WHERE subjectId = :subjectId AND date BETWEEN :start AND :end GROUP BY typeId""")
    fun getDetailedAttendancesBetweenDates(start: Long, end: Long, subjectId: Int): List<AttendanceTypeInfo>

    @Query("""SELECT AttendanceTypes.id, AttendanceTypes.name, count(Attendances.id) AS count, color FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                INNER JOIN EventTypes ON EventTypes.id = eventTypeId
                WHERE subjectId IS NULL AND date BETWEEN :start AND :end GROUP BY typeId""")
    fun getDetailedAttendancesBetweenDatesNoSubject(start: Long, end: Long): List<AttendanceTypeInfo>


    @Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
    class AttendanceSubjectInfo(val subjectId: Int, val subjectName: String, val presents: Int, val total: Int) {
        @Ignore
        val percentage = presents.toFloat().times(100f).div(total.toFloat())
    }

    @Query("""SELECT IFNULL(Subjects.id, 0) as subjectId, IFNULL(Subjects.name, "Inne wydarzenia") as subjectName,
SUM(CASE WHEN countAs = "P" THEN 1 ELSE 0 END) as presents,
COUNT(Attendances.id) as total FROM Attendances
INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN EventTypes ON EventTypes.id = eventTypeId
LEFT OUTER JOIN Subjects ON Subjects.id = subjectId
WHERE date BETWEEN :start AND :end GROUP BY subjectId
ORDER BY Subjects.abbreviation""")
    fun getAttendanceSubjectInfoBetweenDates(start: Long, end: Long): List<AttendanceSubjectInfo>


    class AttendanceLesson(val title: String?, val date: Long, val number: Int)

    @Query("""SELECT IFNULL(Subjects.name, Events.name) as title,
         date, IFNULL(number, -1) as number FROM Attendances
INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN EventTypes ON EventTypes.id = eventTypeId
LEFT OUTER JOIN Subjects ON Subjects.id = subjectId
WHERE subjectId = :subjectId AND typeId = :typeId
AND date BETWEEN :start AND :end
ORDER BY date, number""")
    fun getAttendanceDatesBySubject(start: Long, end: Long, subjectId: Int, typeId: Int): List<AttendanceLesson>

    @Query("""SELECT IFNULL(Subjects.name, Events.name) as title,
         date, IFNULL(number, -1) as number FROM Attendances
INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN EventTypes ON EventTypes.id = eventTypeId
LEFT OUTER JOIN Subjects ON Subjects.id = subjectId
WHERE subjectId IS NULL AND typeId = :typeId
AND date BETWEEN :start AND :end
ORDER BY date, number""")
    fun getAttendanceDatesByNullSubject(start: Long, end: Long, typeId: Int): List<AttendanceLesson>

    @Query("""SELECT IFNULL(Subjects.name, Events.name) as title,
         date, IFNULL(number, -1) as number FROM Attendances
INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN EventTypes ON EventTypes.id = eventTypeId
LEFT OUTER JOIN Subjects ON Subjects.id = subjectId
WHERE typeId = :typeId AND date BETWEEN :start AND :end
ORDER BY date, number""")
    fun getAttendanceDatesByAnySubject(start: Long, end: Long, typeId: Int): List<AttendanceLesson>


}
