package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Transaction


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
}