package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Query
import jakubweg.mobishit.helper.DateHelper

@Suppress("FunctionName")
@Dao
interface MarkDao {

    companion object {
        const val PARENT_TYPE_COUNT_BEST_MARK = 5
        const val PARENT_TYPE_COUNT_AVERAGE = 3
        const val PARENT_TYPE_UNKNOWN_USED_BY_KOCOL = 1
    }

    class SubjectShortInfo(val id: Int, val name: String) {
        @Ignore
        var averageText: String = ""
    }

    @Query("""SELECT Subjects.id, Subjects.name FROM Marks
                    INNER JOIN MarkGroups ON Marks.markGroupId = MarkGroups.id
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    GROUP BY Subjects.id ORDER BY Subjects.name""")
    fun getSubjectsWithUsersMarks(): List<SubjectShortInfo>

    class TermShortInfo(val id: Int, val name: String, val type: String)

    @Query("""SELECT Terms.id, Terms.name, Terms.type FROM Terms ORDER BY Terms.type""")
    fun getTerms(): List<TermShortInfo>


    class MarkShortInfo(val id: Int, val description: String, val abbreviation: String?, markScaleValue: Float?,
                        defaultWeight: Float?, noCountToAverage: Boolean?, markPointsValue: Float?,
                        countPointsWithoutBase: Boolean?, markValueMax: Float?, val termId: Int,
                        parentType: Int?, parentId: Int?, markGroupId: Int) :
            MarkAverageShortInfo(markScaleValue, parentType, parentId, markGroupId, defaultWeight, noCountToAverage,
                    markPointsValue, countPointsWithoutBase, markValueMax)

    @Query("""SELECT
                        Marks.id, MarkGroups.description, MarkScales.abbreviation, MarkScales.markValue AS 'markScaleValue',
                        MarkKinds.defaultWeight, MarkScales.noCountToAverage, Marks.markValue AS 'markPointsValue',
                        MarkGroups.countPointsWithoutBase, MarkGroups.markValueMax, Terms.id AS 'termId',
                        parentType, MarkGroups.parentId, MarkGroups.id AS markGroupId
                    FROM Marks
                    LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
                    INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    INNER JOIN Terms ON Terms.id = EventTypeTerms.termId
                    INNER JOIN MarkKinds ON MarkGroups.markKindId = MarkKinds.id
                    WHERE Subjects.id = :subjectId""")
    fun getMarksBySubject(subjectId: Int): List<MarkShortInfo>

    open class MarkAverageShortInfo(val markScaleValue: Float?, val parentType: Int?, val parentId: Int?, val markGroupId: Int,
                                    val defaultWeight: Float?, val noCountToAverage: Boolean?, val markPointsValue: Float?,
                                    val countPointsWithoutBase: Boolean?, val markValueMax: Float?) {
        @Ignore
        var hasCalculatedAverage = false
    }

    @Query("""SELECT
                        MarkScales.markValue AS 'markScaleValue',
                        MarkKinds.defaultWeight, MarkScales.noCountToAverage, Marks.markValue AS 'markPointsValue',
                        MarkGroups.countPointsWithoutBase, MarkGroups.markValueMax,
                        parentType, MarkGroups.parentId, MarkGroups.id AS markGroupId
                    FROM Marks
                    LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
                    INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    INNER JOIN Terms ON Terms.id = EventTypeTerms.termId
                    INNER JOIN MarkKinds ON MarkGroups.markKindId = MarkKinds.id
                    WHERE Subjects.id = :subjectId AND Terms.id = :termId""")
    fun getMarksBySubjectAndTerm(termId: Int, subjectId: Int): List<MarkAverageShortInfo>


    class MarkShortInfoWithSubject(val id: Int, val description: String, val abbreviation: String?, val markPointsValue: Float?, val subjectName: String)

    @Query("""SELECT Marks.id, MarkGroups.description, MarkScales.abbreviation,
        Marks.markValue AS markPointsValue, Subjects.name AS subjectName
FROM Marks
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
INNER JOIN EventTypeTerms ON EventTypeTerms.id = MarkGroups.eventTypeTermId
INNER JOIN EventTypes ON EventTypes.id = EventTypeTerms.eventTypeId
INNER JOIN Subjects ON Subjects.id = EventTypes.subjectId
WHERE Marks.id IN (:markIds)""")
    fun getMarkShortInfo(markIds: List<Int>): List<MarkShortInfoWithSubject>


    class MarkDetails(val description: String, val markName: String?, val abbreviation: String?, val markPointsValue: Float?,
                      val columnName: String, val defaultWeight: Float?, val noCountToAverage: Boolean?, val countPointsWithoutBase: Boolean?,
                      val markValueMax: Float?, val getDate: Long, val addTime: Long, val teacherName: String, val teacherSurname: String,
                      val subjectName: String?, val parentType: Int?) {
        @Ignore
        val formattedGetDate = DateHelper.millisToStringDate(getDate)
        @Ignore
        val formattedAddTime = DateHelper.millisToStringTime(addTime)
    }

    @Query("""SELECT MarkGroups.description, MarkScales.name AS markName, MarkScales.abbreviation, Marks.markValue AS markPointsValue,
         MarkKinds.name AS columnName, MarkKinds.defaultWeight, noCountToAverage, countPointsWithoutBase,
         markValueMax, getDate, addTime, Teachers.name AS teacherName, Teachers.surname AS teacherSurname,
         Subjects.name AS subjectName, parentType
FROM Marks
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
INNER JOIN Terms ON Terms.id = EventTypeTerms.termId
INNER JOIN MarkKinds ON MarkGroups.markKindId = MarkKinds.id
INNER JOIN Teachers ON Teachers.id = Marks.teacherId
WHERE Marks.id = :markId LIMIT 1""")
    fun getMarkDetails(markId: Int): MarkDetails


    class DeletedMarkData(val description: String?, val abbreviation: String?, val subjectName: String)

    @Query("""SELECT MarkGroups.description, MarkScales.abbreviation, Subjects.name as subjectName FROM Marks
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
WHERE Marks.id = :id LIMIT 1""")
    fun getDeletedMarkInfo(id: Int): DeletedMarkData
}
