package jakubweg.mobishit.db

import android.arch.persistence.room.*
import android.util.SparseArray
import jakubweg.mobishit.helper.DateHelper

@Suppress("FunctionName")
@Dao
interface MarkDao {

    companion object {
        const val PARENT_TYPE_COUNT_EVERY = 1
        const val PARENT_TYPE_COUNT_LAST = 2
        const val PARENT_TYPE_COUNT_AVERAGE = 3
        const val PARENT_TYPE_COUNT_WORSE = 4 //lol, i don't know why xD
        const val PARENT_TYPE_COUNT_BEST = 5

        val parentTypesAsText = SparseArray<String>(5).also {
            it.put(MarkDao.PARENT_TYPE_COUNT_EVERY, "Liczy się każda ocena")
            it.put(MarkDao.PARENT_TYPE_COUNT_AVERAGE, "Liczy się średnia z ocen")
            it.put(MarkDao.PARENT_TYPE_COUNT_LAST, "Liczy się ostatnia ocena")
            it.put(MarkDao.PARENT_TYPE_COUNT_BEST, "Liczy się lepsza ocena")
            it.put(MarkDao.PARENT_TYPE_COUNT_WORSE, "Liczy się gorsza ocena")
        }
    }


    class SubjectShortInfo(val id: Int, val name: String) {
        override fun toString() = name
    }

    @Query("""SELECT Subjects.id as id, Subjects.name FROM Marks
                    INNER JOIN MarkGroups ON Marks.markGroupId = MarkGroups.id
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    WHERE addTime BETWEEN :dateStart AND :dateEnd
                    GROUP BY Subjects.id ORDER BY Subjects.name""")
    fun getSubjectsWithUsersMarks(dateStart: Long, dateEnd: Long): List<SubjectShortInfo>


    // this is used by average calculations
    class MarkShortInfo(val id: Int, val description: String, val abbreviation: String?, val parentType: Int?,
                        val parentId: Int, val markGroupId: Int, val weight: Float, val noCountToAverage: Boolean?,
                        val markPointsValue: Float, val countPointsWithoutBase: Boolean?, val markValueMax: Float,
                        val addTime: Long, val termId: Int, val markScaleValue: Float) {
        val parentIdOrSelf: Int get() = if (parentId < 0) markGroupId else parentId
        @Ignore
        var viewType = 0
        @Ignore
        var hasCalculatedAverage = false
    }

    @Query("""SELECT
                        Marks.id, MarkGroups.description, MarkScales.abbreviation, IFNULL(MarkScales.markValue, -1) AS 'markScaleValue',
                        IFNULL(weight, IFNULL(MarkKinds.defaultWeight, -1)) as 'weight', MarkScales.noCountToAverage,
                        IFNULL(Marks.markValue, -1) AS 'markPointsValue',
                        MarkGroups.countPointsWithoutBase, IFNULL(MarkGroups.markValueMax, -1) as 'markValueMax', Terms.id AS 'termId',
                        parentType, IFNULL(MarkGroups.parentId, -1) as 'parentId', MarkGroups.id AS markGroupId, addTime
                    FROM Marks
                    LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
                    INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    INNER JOIN Terms ON Terms.id = EventTypeTerms.termId
                    INNER JOIN MarkKinds ON MarkGroups.markKindId = MarkKinds.id
                    WHERE Subjects.id = :subjectId AND MarkGroups.visibility = 0
                     AND (:termId IS NULL OR Terms.id = :termId OR Terms.parentId = :termId)
                    ORDER BY getDate DESC""")
    fun getMarksBySubject(subjectId: Int, termId: Int?): List<MarkShortInfo>


    /// this class is used to show notifications
    class MarkShortInfoWithSubject(val id: Int, val description: String, val abbreviation: String?, val markPointsValue: Float, val subjectName: String)

    @Query("""SELECT Marks.id, MarkGroups.description, MarkScales.abbreviation,
        IFNULL(Marks.markValue, -1) AS 'markPointsValue', Subjects.name AS subjectName
FROM Marks
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
INNER JOIN EventTypeTerms ON EventTypeTerms.id = MarkGroups.eventTypeTermId
INNER JOIN EventTypes ON EventTypes.id = EventTypeTerms.eventTypeId
INNER JOIN Subjects ON Subjects.id = EventTypes.subjectId
WHERE Marks.id IN (:markIds) AND visibility = 0""")
    fun getMarkShortInfo(markIds: List<Int>): List<MarkShortInfoWithSubject>


    /// used in MarkDetailsFragment
    class MarkDetails(val description: String, val markName: String?, val abbreviation: String?, val markPointsValue: Float,
                      val columnName: String, val defaultWeight: Float?, val noCountToAverage: Boolean?, val countPointsWithoutBase: Boolean?,
                      val markValueMax: Float, val getDate: Long, val addTime: Long, val teacherName: String, val teacherSurname: String,
                      val subjectName: String?, val parentType: Int?) {
        @Suppress("unused")
        @Ignore
        val formattedGetDate = DateHelper.millisToStringDate(getDate)
        @Ignore
        val formattedAddTime = DateHelper.millisToStringTime(addTime)
    }

    @Query("""SELECT MarkGroups.description, MarkScales.name AS markName, MarkScales.abbreviation, IFNULL(Marks.markValue, -1) AS markPointsValue,
         MarkKinds.name AS columnName, MarkKinds.defaultWeight, noCountToAverage, countPointsWithoutBase,
         IFNULL(markValueMax, -1) as 'markValueMax', getDate, addTime, Teachers.name AS teacherName, Teachers.surname AS teacherSurname,
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
    fun getMarkDetails(markId: Int): MarkDetails?


    class DeletedMarkData(val description: String?, val abbreviation: String?, val subjectName: String)

    @Query("""SELECT MarkGroups.description, MarkScales.abbreviation, Subjects.name as subjectName FROM Marks
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
WHERE Marks.id = :id AND visibility = 0 LIMIT 1""")
    fun getDeletedMarkInfo(id: Int): DeletedMarkData


    class MarkScaleShortInfo(val id: Int, val abbreviation: String, val markValue: Float)

    @Query("""SELECT id, abbreviation, markValue FROM MarkScales
WHERE MarkScales.markScaleGroupId = :groupId AND not noCountToAverage AND length(abbreviation) > 0
ORDER BY markValue""")
    fun getMarkScalesByGroupId(groupId: Int): List<MarkScaleShortInfo>


    @Query("DELETE FROM AverageCaches")
    fun clearAverageCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAverageCache(objects: List<AverageCacheData>)

    @Query("SELECT * FROM AverageCaches")
    fun getAllAverageCache(): List<AverageCacheData>


    @Query("""SELECT Marks.id, MarkGroups.description || ' • ' || Subjects.name as description,
        IFNULL(MarkScales.abbreviation, Marks.markValue) as value, addTime FROM Marks
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
INNER JOIN MarkGroups ON MarkGroups.id = Marks.markGroupId
INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
ORDER BY addTime DESC LIMIT :count""")
    fun getLastMarks(count: Int): List<LastMarkCacheData>

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertLastMarks(values: List<LastMarkCacheData>)

    @Query("SELECT * FROM LastMarksCache ORDER BY addTime DESC LIMIT :limit")
    fun getCachedLastMarks(limit: Int): List<LastMarkCacheData>

    @Query("DELETE FROM LastMarksCache")
    fun deleteCachedLastMarks()


    @Query("UPDATE Subjects SET isExcludedFromStats = :isExcluding WHERE id = :subjectId")
    fun updateSubjectExcluding(subjectId: Int, isExcluding: Boolean)


    class MarkScaleGroupShortInfo(val id: Int, val name: String) {
        override fun toString() = name
    }

    @Query("""SELECT id, name FROM MarkScaleGroups
WHERE markType = 'P'
ORDER BY (isPublic + isDefault) + id * 100 DESC""")
    fun getUsedMarkScaleGroups(): List<MarkScaleGroupShortInfo>


    @Query("""SELECT MarkScaleGroups.id, IFNULL(MarkScaleGroups.name, "??") as name FROM Marks
INNER JOIN MarkGroups ON Marks.markGroupId = MarkGroups.id
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
LEFT OUTER JOIN MarkScaleGroups ON MarkScaleGroups.id = MarkGroups.markScaleGroupId
INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
WHERE (MarkGroups.countPointsWithoutBase = 0 OR MarkGroups.countPointsWithoutBase IS NULL)
    AND (MarkScales.noCountToAverage = 0 OR MarkScales.noCountToAverage IS NULL)
	AND subjectId = :subjectId
GROUP BY MarkScaleGroups.id
ORDER BY MarkScaleGroups.id DESC""")
    fun getUsedMarkScaleGroupsBySubject(subjectId: Int): List<MarkScaleGroupShortInfo>


    @Query("""SELECT Subjects.id as id, Subjects.name FROM Marks
                    INNER JOIN MarkGroups ON Marks.markGroupId = MarkGroups.id
                    LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
                    INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
                    INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
                    INNER JOIN Subjects ON EventTypes.subjectId = Subjects.id
                    WHERE (MarkGroups.countPointsWithoutBase = 0 OR MarkGroups.countPointsWithoutBase IS NULL)
                        AND (MarkScales.noCountToAverage = 0 OR MarkScales.noCountToAverage IS NULL)
                    GROUP BY Subjects.id ORDER BY Subjects.name""")
    fun getSubjectsWithCountedUsersMarks(): List<SubjectShortInfo>


    class MarkToImport(val markValue: Float?, val scaleId: Int?,
                       val weight: Float, val parentType: Int?, val parentId: Int)

    @Query("""SELECT Marks.markValue, MarkScales.id as scaleId,
    IFNULL(MarkGroups.markValueMax * NOT MarkGroups.countPointsWithoutBase,
        IFNULL(MarkGroups.weight,MarkKinds.defaultWeight)) as weight,
parentType, IFNULL(parentId, MarkGroups.id) as parentId
FROM Marks
INNER JOIN MarkGroups ON Marks.markGroupId = MarkGroups.id
LEFT OUTER JOIN MarkScales ON MarkScales.id = Marks.markScaleId
LEFT OUTER JOIN MarkScaleGroups ON MarkScaleGroups.id = MarkGroups.markScaleGroupId
INNER JOIN MarkKinds ON MarkKinds.id = MarkGroups.markKindId
INNER JOIN EventTypeTerms ON MarkGroups.eventTypeTermId = EventTypeTerms.id
INNER JOIN EventTypes ON EventTypeTerms.eventTypeId = EventTypes.id
WHERE (MarkScales.noCountToAverage = 0 OR MarkScales.noCountToAverage IS NULL)
	AND subjectId = :subjectId AND MarkScaleGroups.id = :markScaleGroupId
    AND addTime BETWEEN :startTime AND :endTime
ORDER BY addTime DESC""")
    fun getMarksToImport(subjectId: Int, markScaleGroupId: Int, startTime: Long, endTime: Long): List<MarkToImport>


    @Query("DELETE FROM SavedVirtualMarks")
    fun clearVirtualMarks()

    @Insert()
    fun insertVirtualMarks(values: List<VirtualMarkEntity>)

    @Query("SELECT * FROM SavedVirtualMarks")
    fun getVirtualMarksEntities(): List<VirtualMarkEntity>
}
