package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Query
import jakubweg.mobishit.model.AboutAttendancesModel

@Dao
interface AttendanceDao {

    class TypeInfo(val name: String, val color: Int, val countAs: String) : AboutAttendancesModel.TypeInfoAboutItemParent()

    @Query("SELECT name, color, countAs FROM AttendanceTypes ORDER BY countAs, name")
    fun getTypesInfo(): List<TypeInfo>



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
                INNER JOIN EventTypes ON EventTypes.id = eventTypeId
                INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
                WHERE date BETWEEN :start AND :end AND isExcludedFromStats = 0
                AND status != 2 AND substitution != 1 GROUP BY countAs""")
    fun getAttendancesBetweenDates(start: Long, end: Long): List<AttendanceCountInfo>

    @Query("""SELECT countAs, count(Attendances.id) AS count FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                INNER JOIN EventTypes ON EventTypes.id = eventTypeId
                INNER JOIN Terms ON Terms.id = Events.termId
                INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
                WHERE (Terms.id = :termId OR Terms.parentId = :termId) AND isExcludedFromStats = 0
                AND status != 2 AND substitution != 1 GROUP BY countAs""")
    fun getAttdnadancesByTerm(termId: Int): List<AttendanceCountInfo>


    class AttendanceTypeAndCountInfo(val id: Int, val name: String, val count: Int, val color: Int)

    @Query("""SELECT AttendanceTypes.id, AttendanceTypes.name, count(Attendances.id) AS count, color FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                INNER JOIN EventTypes ON EventTypes.id = eventTypeId
                INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
                WHERE date BETWEEN :start AND :end AND isExcludedFromStats = 0
                AND status != 2 AND substitution != 1 GROUP BY typeId""")
    fun getDetailedAttendancesBetweenDates(start: Long, end: Long): List<AttendanceTypeAndCountInfo>

    @Query("""SELECT AttendanceTypes.id, AttendanceTypes.name, count(Attendances.id) AS count, color FROM Attendances
                INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
                INNER JOIN Events ON Events.id = Attendances.eventId
                INNER JOIN EventTypes ON EventTypes.id = eventTypeId
                INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
                WHERE subjectId = :subjectId AND date BETWEEN :start AND :end AND isExcludedFromStats = 0
                AND status != 2 AND substitution != 1 GROUP BY typeId""")
    fun getDetailedAttendancesBetweenDates(start: Long, end: Long, subjectId: Int): List<AttendanceTypeAndCountInfo>


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
INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
WHERE date BETWEEN :start AND :end AND status != 2 AND substitution != 1 AND isExcludedFromStats = 0
GROUP BY subjectId ORDER BY Subjects.abbreviation""")
    fun getAttendanceSubjectInfoBetweenDates(start: Long, end: Long): List<AttendanceSubjectInfo>


    class AttendanceLesson(val title: String?, val date: Long, val number: Int)

    @Query("""SELECT IFNULL(Subjects.name, Events.name) as title,
         date, IFNULL(number, -1) as number FROM Attendances
INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN EventTypes ON EventTypes.id = eventTypeId
INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
WHERE subjectId = :subjectId AND typeId = :typeId AND status != 2 AND substitution != 1
AND date BETWEEN :start AND :end
ORDER BY date, number""")
    fun getAttendanceDatesBySubject(start: Long, end: Long, subjectId: Int, typeId: Int): List<AttendanceLesson>

    @Query("""SELECT IFNULL(Subjects.name, Events.name) as title,
         date, IFNULL(number, -1) as number FROM Attendances
INNER JOIN AttendanceTypes ON AttendanceTypes.id = typeId
INNER JOIN Events ON Events.id = Attendances.eventId
INNER JOIN EventTypes ON EventTypes.id = eventTypeId
INNER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)
WHERE typeId = :typeId AND date BETWEEN :start AND :end AND status != 2 AND substitution != 1 AND isExcludedFromStats = 0
ORDER BY date, number""")
    fun getAttendanceDatesByAnySubject(start: Long, end: Long, typeId: Int): List<AttendanceLesson>
}
