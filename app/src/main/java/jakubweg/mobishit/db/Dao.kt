package jakubweg.mobishit.db

import android.arch.persistence.room.*


@Dao
interface MainDao {
    //inserts

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: Teacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: RoomData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: TermData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: SubjectData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: GroupData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: GroupTerm)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkScaleGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkScale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkDivisionGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkKind)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkGroupGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventTypeTeacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventTypeTerm)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventTypeGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventIssue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: AttendanceType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: AttendanceData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MarkData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: StudentGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: EventTypeSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: LessonData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(value: MessageData)


    //inserts


    @Transaction
    fun insertAny(value: Any) {
        when (value) {
            is Teacher -> insert(value)
            is RoomData -> insert(value)
            is TermData -> insert(value)
            is SubjectData -> insert(value)
            is GroupData -> insert(value)
            is GroupTerm -> insert(value)
            is MarkScaleGroup -> insert(value)
            is MarkScale -> insert(value)
            is MarkDivisionGroup -> insert(value)
            is MarkKind -> insert(value)
            is MarkGroupGroup -> insert(value)
            is MarkGroup -> insert(value)
            is EventType -> insert(value)
            is EventTypeTeacher -> insert(value)
            is EventTypeTerm -> insert(value)
            is EventTypeGroup -> insert(value)
            is EventData -> insert(value)
            is EventIssue -> insert(value)
            is EventEvent -> insert(value)
            is AttendanceType -> insert(value)
            is AttendanceData -> insert(value)
            is MarkData -> insert(value)
            is StudentGroup -> insert(value)
            is EventTypeSchedule -> insert(value)
            is LessonData -> insert(value)
            is MessageData -> insert(value)
            else -> throw IllegalArgumentException()
        }
    }


    // deletions

    /*@Transaction
    private fun deleteEntity(db: AppDatabase,tableName: String, id: Int) {
        db.query("DELETE FROM ? WHERE id = ?", arrayOf(tableName, id))
    }*/

    @Delete
    fun delete(value: Teacher)

    @Delete
    fun delete(value: RoomData)

    @Delete
    fun delete(value: TermData)

    @Delete
    fun delete(value: SubjectData)

    @Delete
    fun delete(value: GroupData)

    @Delete
    fun delete(value: GroupTerm)

    @Delete
    fun delete(value: MarkScaleGroup)

    @Delete
    fun delete(value: MarkScale)

    @Delete
    fun delete(value: MarkDivisionGroup)

    @Delete
    fun delete(value: MarkKind)

    @Delete
    fun delete(value: MarkGroupGroup)

    @Delete
    fun delete(value: MarkGroup)

    @Delete
    fun delete(value: EventType)

    @Delete
    fun delete(value: EventTypeTeacher)

    @Delete
    fun delete(value: EventTypeTerm)

    @Delete
    fun delete(value: EventTypeGroup)

    @Delete
    fun delete(value: EventData)

    @Delete
    fun delete(value: EventIssue)

    @Delete
    fun delete(value: EventEvent)

    @Delete
    fun delete(value: AttendanceType)

    @Delete
    fun delete(value: AttendanceData)

    @Delete
    fun delete(value: MarkData)

    @Delete
    fun delete(value: StudentGroup)

    @Delete
    fun delete(value: EventTypeSchedule)

    @Delete
    fun delete(value: LessonData)

    @Delete
    fun delete(value: MessageData)

    @Transaction
    fun deleteAny(value: Any) {
        when (value) {
            is Teacher -> delete(value)
            is RoomData -> delete(value)
            is TermData -> delete(value)
            is SubjectData -> delete(value)
            is GroupData -> delete(value)
            is GroupTerm -> delete(value)
            is MarkScaleGroup -> delete(value)
            is MarkScale -> delete(value)
            is MarkDivisionGroup -> delete(value)
            is MarkKind -> delete(value)
            is MarkGroupGroup -> delete(value)
            is MarkGroup -> delete(value)
            is EventType -> delete(value)
            is EventTypeTeacher -> delete(value)
            is EventTypeTerm -> delete(value)
            is EventTypeGroup -> delete(value)
            is EventData -> delete(value)
            is EventIssue -> delete(value)
            is EventEvent -> delete(value)
            is AttendanceType -> delete(value)
            is AttendanceData -> delete(value)
            is MarkData -> delete(value)
            is StudentGroup -> delete(value)
            is EventTypeSchedule -> delete(value)
            is LessonData -> delete(value)
            is MessageData -> delete(value)
            else -> throw IllegalArgumentException()
        }
    }
}