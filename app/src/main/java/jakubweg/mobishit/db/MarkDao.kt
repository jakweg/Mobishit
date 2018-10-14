package jakubweg.mobishit.db

import android.arch.persistence.room.*
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.helper.DateHelper

@Suppress("FunctionName")
@Dao
interface MarkDao {

    class SubjectShortInfo(val id: Int, val name: String, val marksCount: Int)

    @Query("""SELECT Subjects.id, Subjects.name, COUNT(Marks.id) as marksCount FROM Marks
                    INNER JOIN MarkGroups ON Marks.markGroupId = MarkGroups.id
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    GROUP BY Subjects.id ORDER BY Subjects.name""")
    fun getSubjectsWithUsersMarks(): List<SubjectShortInfo>

    class TermShortInfo(val id: Int, val name: String, val type: String)

    @Query("""SELECT Terms.id, Terms.name, Terms.type FROM Terms ORDER BY Terms.type""")
    fun getTerms(): List<TermShortInfo>


    @Query("""SELECT
                        Marks.id, MarkGroups.description, MarkScales.abbreviation, MarkScales.markValue AS 'markScaleValue', MarkKinds.defaultWeight, MarkScales.noCountToAverage, Marks.markValue AS 'markPointsValue', MarkGroups.countPointsWithoutBase, MarkGroups.markValueMax, Terms.id AS 'termId'
                    FROM Marks
                    LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
                    INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    INNER JOIN Terms ON Terms.id = EventTypeTerms.termId
                    INNER JOIN MarkKinds ON MarkGroups.markKindId = MarkKinds.id
                    WHERE Subjects.id = :subjectId""")
    fun _obtainMarksToTempTable(subjectId: Int): List<MarkShortData>

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun _insertMarksToTempTable(values: List<MarkShortData>)

    @Query("DELETE FROM Temp_Marks")
    fun _deleteTempTable()

    class AverageCalculationResult(val weightedAverage: Float?, val gotPointsSum: Float?, val baseSum: Float?)

    @Query("""SELECT
                        SUM(Temp_Marks.markScaleValue * Temp_Marks.defaultWeight) / SUM(Temp_Marks.defaultWeight) AS weightedAverage,
                        SUM(Temp_Marks.markPointsValue) AS gotPointsSum,
                        SUM(Temp_Marks.markValueMax * NOT Temp_Marks.countPointsWithoutBase) AS baseSum
                    FROM Temp_Marks WHERE (Temp_Marks.noCountToAverage IS NULL OR Temp_Marks.noCountToAverage = 0) AND Temp_Marks.termId = :termId""")
    fun _calculateAverageFromTempTableByTerm(termId: Int): AverageCalculationResult

    @Query("""SELECT
                        SUM(Temp_Marks.markScaleValue * Temp_Marks.defaultWeight) / SUM(Temp_Marks.defaultWeight) AS weightedAverage,
                        SUM(Temp_Marks.markPointsValue) AS gotPointsSum,
                        SUM(Temp_Marks.markValueMax * NOT Temp_Marks.countPointsWithoutBase) AS baseSum
                    FROM Temp_Marks WHERE Temp_Marks.noCountToAverage IS NULL OR Temp_Marks.noCountToAverage = 0""")
    fun _calculateAverageFromTempTableForAll(): AverageCalculationResult


    class MarkShortInfo(val id: Int, val description: String, val abbreviation: String?, val markPointsValue: Float?)

    @Query("SELECT Temp_Marks.id, Temp_Marks.description, Temp_Marks.abbreviation, Temp_Marks.markPointsValue FROM Temp_Marks WHERE Temp_Marks.termId = :termId")
    fun _getMarksFromTempTableByTerm(termId: Int): List<MarkShortInfo>

    @Query("SELECT Temp_Marks.id, Temp_Marks.description, Temp_Marks.abbreviation, Temp_Marks.markPointsValue FROM Temp_Marks")
    fun _getAllMarksFromTempTable(): List<MarkShortInfo>

    @Transaction
    fun getMarksBySubjectAndTerm(subjectId: Int, termId: Int?): Pair<AverageCalculationResult, List<MarkShortInfo>> {
        _deleteTempTable()
        _insertMarksToTempTable(_obtainMarksToTempTable(subjectId))
        val result =
                if (termId == null) Pair(_calculateAverageFromTempTableForAll(), _getAllMarksFromTempTable())
                else Pair(_calculateAverageFromTempTableByTerm(termId), _getMarksFromTempTableByTerm(termId))
        if (!BuildConfig.DEBUG) _deleteTempTable()
        return result
    }

    @Query("""SELECT
                        :markId AS id, MarkGroups.description, MarkScales.abbreviation, Marks.markValue AS markPointsValue
                    FROM Marks
                    LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
                    INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
                    WHERE Marks.id = :markId LIMIT 1""")
    fun getMarkShortInfo(markId: Int): MarkShortInfo

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
                      val markValueMax: Float?, val getDate: Long, val addTime: Long, val teacherName: String, val teacherSurname: String) {
        @Ignore
        val formattedGetDate = DateHelper.millisToStringDate(getDate)
        @Ignore
        val formattedAddTime = DateHelper.millisToStringTime(addTime)
    }

    @Query("""SELECT MarkGroups.description, MarkScales.name AS markName, MarkScales.abbreviation, Marks.markValue AS markPointsValue,
         MarkKinds.name AS columnName, MarkKinds.defaultWeight, MarkScales.noCountToAverage, MarkGroups.countPointsWithoutBase,
         MarkGroups.markValueMax, Marks.getDate, Marks.addTime, Teachers.name AS teacherName, Teachers.surname AS teacherSurname
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
}
