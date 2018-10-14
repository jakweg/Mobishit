package jakubweg.mobishit.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "Temp_Marks")
data class MarkShortData(@PrimaryKey(autoGenerate = false) val id: Int, val description: String, val abbreviation: String?, val markScaleValue: Float?, val defaultWeight: Int?, val noCountToAverage: Boolean?, val markPointsValue: Float?, val countPointsWithoutBase: Boolean, val markValueMax: Float?, val termId: Int)

@Entity(tableName = "Teachers")
data class Teacher(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val surname: String, val login: String)

@Entity(tableName = "Rooms")
data class RoomData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val description: String)

@Entity(tableName = "Terms")
data class TermData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val type: String, val startDate: Long, val endDate: Long)

@Entity(tableName = "Subjects")
data class SubjectData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, @SerializedName("abbr") val abbreviation: String)

@Entity(tableName = "Groups")
data class GroupData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val parentId: Int, val type: String, @SerializedName("abbr") val abbreviation: String)

@Entity(tableName = "GroupsTerms")
data class GroupTerm(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val termId: Int)

@Entity(tableName = "MarkScaleGroups")
data class MarkScaleGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String)

@Entity(tableName = "MarkScales")
data class MarkScale(@PrimaryKey(autoGenerate = true) val id: Int, val markScaleGroupId: Int, val abbreviation: String, val name: String, val markValue: Float, val noCountToAverage: Boolean)

@Entity(tableName = "MarkDivisionGroups")
data class MarkDivisionGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val type: Int, val rangeMin: Float, val rangeMax: Float, val markScaleGroupsId: Int)

@Entity(tableName = "MarkKinds")
data class MarkKind(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val abbreviation: String, val defaultMarkType: Int?, val defaultMarkScaleGroupId: Int?, val defaultWeight: Int?, val position: Int, val cssStyle: String)

@Entity(tableName = "MarkGroupGroups")
data class MarkGroupGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val position: Int)

@Entity(tableName = "MarkGroups")
data class MarkGroup(@PrimaryKey(autoGenerate = true) val id: Int, val markKindId: Int, val markScaleGroupId: Int?, val eventTypeTermId: Int, val abbreviation: String, val description: String, val markType: Int, val position: Int, val countPointsWithoutBase: Boolean, val markValueMin: Int?, val markValueMax: Int?)

@Entity(tableName = "EventTypes")
data class EventType(@PrimaryKey(autoGenerate = true) val id: Int, val subjectId: Int?)

@Entity(tableName = "EventTypeTeachers")
data class EventTypeTeacher(@PrimaryKey(autoGenerate = true) val id: Int, val teacherId: Int, val eventTypeId: Int)

@Entity(tableName = "EventTypeTerms")
data class EventTypeTerm(@PrimaryKey(autoGenerate = true) val id: Int, val termId: Int, val eventTypeId: Int)

@Entity(tableName = "EventTypeGroups")
data class EventTypeGroup(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val eventTypeId: Int)

@Entity(tableName = "Events")
data class EventData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String?, val date: Long, val number: Int?, val startTime: String, val endTime: String, val roomId: Int?, val eventTypeId: Int, val status: Int, val substitution: Int, val termId: Int, val lessonGroupId: Int)

@Entity(tableName = "EventIssues")
data class EventIssue(@PrimaryKey(autoGenerate = true) val id: Int, val eventId: Int, val issueId: Int, val eventTypeId: Int)

@Entity(tableName = "EventEvents")
data class EventEvent(@PrimaryKey(autoGenerate = true) val id: Int, val event1Id: Int, val event2Id: Int)

@Entity(tableName = "AttendanceTypes")
data class AttendanceType(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, @SerializedName("abbr") val abbreviation: String, val color: Int, val countAs: String, val type: String)

@Entity(tableName = "Attendances")
data class AttendanceData(@PrimaryKey(autoGenerate = true) val id: Int, val eventId: Int, val typeId: Int)

@Entity(tableName = "Marks")
data class MarkData(@PrimaryKey(autoGenerate = true) val id: Int, val markGroupId: Int, val markScaleId: Int?, val teacherId: Int, val markValue: Float?, val getDate: Long, val addTime: Long)

@Entity(tableName = "StudentGroups")
data class StudentGroup(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val number: Int)

@Entity(tableName = "EventTypeSchedules")
data class EventTypeSchedule(@PrimaryKey(autoGenerate = true) val id: Int, val eventTypeId: Int, val scheduleId: Int, val name: String, val number: String)

@Entity(tableName = "Lessons")
data class LessonData(@PrimaryKey(autoGenerate = true) val id: Int, val lessonNumber: Int, val startTime: String, val endTime: String)

@Entity(tableName = "Messages")
data class MessageData(@PrimaryKey(autoGenerate = true) val id: Int, val kind: Int, val sendTime: Long, val senderId: Int, val title: String?, val content: String, var readTime: Long)




