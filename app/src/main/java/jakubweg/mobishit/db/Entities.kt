package jakubweg.mobishit.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.support.v4.content.res.ResourcesCompat
import android.text.SpannableString
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


@Entity(tableName = "Teachers")
class Teacher(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val surname: String, val login: String, val sex: String)

@Entity(tableName = "Rooms")
class RoomData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val description: String)

@Entity(tableName = "Terms")
class TermData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val type: String, val parentId: Int?, val startDate: Long, val endDate: Long)

@Entity(tableName = "Subjects")
class SubjectData(@PrimaryKey(autoGenerate = false) val id: Int, val name: String, @SerializedName("abbr") val abbreviation: String, val isExcludedFromStats: Boolean)

@Entity(tableName = "Groups")
class GroupData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val parentId: Int, val type: String, @SerializedName("abbr") val abbreviation: String)

@Entity(tableName = "GroupsTerms")
class GroupTerm(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val termId: Int)

@Entity(tableName = "MarkScaleGroups")
class MarkScaleGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val markType: String, val isPublic: Boolean, val isDefault: Boolean)

@Entity(tableName = "MarkScales")
class MarkScale(@PrimaryKey(autoGenerate = true) val id: Int, val markScaleGroupId: Int, val abbreviation: String, val name: String, val markValue: Float, val noCountToAverage: Boolean)

@Entity(tableName = "MarkDivisionGroups")
class MarkDivisionGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val type: Int, val isPublic: Boolean, val rangeMin: Float, val rangeMax: Float, val markScaleGroupsId: Int)

@Entity(tableName = "MarkKinds")
class MarkKind(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val abbreviation: String, val defaultMarkType: Int?, val defaultMarkScaleGroupId: Int?, val defaultWeight: Int?, val position: Int, val cssStyle: String)

@Entity(tableName = "MarkGroupGroups")
class MarkGroupGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val position: Int)

@Entity(tableName = "MarkGroups")
class MarkGroup(@PrimaryKey(autoGenerate = true) val id: Int, val markKindId: Int, val weight: Float?, val markScaleGroupId: Int?, val eventTypeTermId: Int, val abbreviation: String, val description: String, val markType: Int, val position: Int, val countPointsWithoutBase: Boolean, val markValueMin: Int?, val markValueMax: Int?, val parentId: Int?, val parentType: Int?, val visibility: Int?)

@Entity(tableName = "EventTypes")
class EventType(@PrimaryKey(autoGenerate = true) val id: Int, val subjectId: Int?)

@Entity(tableName = "EventTypeTeachers")
class EventTypeTeacher(@PrimaryKey(autoGenerate = true) val id: Int, val teacherId: Int, val eventTypeId: Int)

@Entity(tableName = "EventTypeTerms")
class EventTypeTerm(@PrimaryKey(autoGenerate = true) val id: Int, val termId: Int, val eventTypeId: Int)

@Entity(tableName = "EventTypeGroups")
class EventTypeGroup(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val eventTypeId: Int)

@Entity(tableName = "Events")
class EventData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String?, val date: Long, val number: Int?, val startTime: String, val endTime: String, val roomId: Int?, val eventTypeId: Int, val status: Int, val substitution: Int, val termId: Int, val lessonGroupId: Int)

@Entity(tableName = "EventIssues")
class EventIssue(@PrimaryKey(autoGenerate = true) val id: Int, val eventId: Int, val issueId: Int, val eventTypeId: Int)

@Entity(tableName = "EventEvents")
class EventEvent(@PrimaryKey(autoGenerate = true) val id: Int, val event1Id: Int, val event2Id: Int)

@Entity(tableName = "AttendanceTypes")
class AttendanceType(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, @SerializedName("abbr") val abbreviation: String, val color: Int, val countAs: String, val type: String)

@Entity(tableName = "Attendances")
class AttendanceData(@PrimaryKey(autoGenerate = true) val id: Int, val eventId: Int, val typeId: Int)

@Entity(tableName = "Marks")
class MarkData(@PrimaryKey(autoGenerate = true) val id: Int, val markGroupId: Int, val markScaleId: Int?, val teacherId: Int, val markValue: Float?, val getDate: Long, val addTime: Long)

@Entity(tableName = "StudentGroups")
class StudentGroup(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val number: Int)

@Entity(tableName = "EventTypeSchedules")
class EventTypeSchedule(@PrimaryKey(autoGenerate = true) val id: Int, val eventTypeId: Int, val scheduleId: Int, val name: String, val number: String)

@Entity(tableName = "Lessons")
class LessonData(@PrimaryKey(autoGenerate = true) val id: Int, val lessonNumber: Int, val startTime: String, val endTime: String)

@Entity(tableName = "Messages")
class MessageData(@PrimaryKey(autoGenerate = true) val id: Int, val kind: Int, val sendTime: Long, val senderId: Int, val title: String?, val content: String, var readTime: Long)

@Entity(tableName = "Tests")
class TestData(@PrimaryKey(autoGenerate = true) val id: Int, val date: String, val group: String, val subject: String, val type: String, val description: String, val addTime: String, val teacher: String) {
    @Ignore
    var isInPast: Boolean = false
}

@Entity(tableName = "AverageCaches")
class AverageCacheData(@PrimaryKey(autoGenerate = true) val id: Int,
                       val subjectId: Int, val subjectName: String?, var marks: String?,
                       var weightedAverage: Float, var gotPointsSum: Float, var baseSum: Float) {

    fun getMarksList(): List<String> {
        return marks?.split('@') ?: emptyList()
    }


    fun setMarksList(list: List<MarkDao.MarkShortInfo>?) {
        marks = list?.joinToString(separator = "@") { it ->
            when {
                it.markScaleValue >= 0 -> it.abbreviation.takeUnless { it.isNullOrBlank() }
                        ?: if (it.markScaleValue == it.markScaleValue.toInt().toFloat())
                            it.markScaleValue.toInt().toString() else "%.1f".format(it.markScaleValue)
                it.markPointsValue >= 0 -> it.markPointsValue.toString()
                else -> ""
            }
        }
    }
}


@Entity(tableName = "ComparisonCaches")
class ComparisonCacheData(@PrimaryKey(autoGenerate = true) val id: Int,
                          val subjectName: String, val averageStudent: String,
                          val averageClass: String, val averageSchool: String,
                          val positionInClass: String?, val maxPositionInClass: String?,
                          val positionInSchool: String?, val maxPositionInSchool: String?,
                          val classImg: String?, val schoolImg: String?) {
    constructor(jo: JsonObject) : this(
            0,
            jo["subject"]!!.asString!!,
            jo["avg_person"]!!.asString!!,
            jo["avg_class"]!!.asString!!,
            jo["avg_school"]!!.asString!!,
            jo["pos_class"].asStringOrNull,
            jo["max_class"].asStringOrNull,
            jo["pos_school"].asStringOrNull,
            jo["max_school"].asStringOrNull,
            jo["img_class"].asStringOrNull,
            jo["img_school"].asStringOrNull
    )
}

@Entity(tableName = "SentMessages")
class SentMessageData(@PrimaryKey(autoGenerate = true) val id: Int,
                      val subject: String,
                      val content: String,
                      val sentTime: Long,
                      val receiverId: Int,
                      val status: Int) {
    companion object {
        const val STATUS_ENQUEUED = 0
        const val STATUS_IN_PROGRESS = 2
        const val STATUS_SUCCEEDED = 3
        const val STATUS_FAILED = 4
        const val STATUS_CANCELLED = 5
    }
}

@Entity(tableName = "LastMarksCache")
class LastMarkCacheData(@PrimaryKey(autoGenerate = false) val id: Int,
                        val description: String,
                        val value: String,
                        val addTime: Long)


@Entity(tableName = "SavedVirtualMarks")
class VirtualMarkEntity(@PrimaryKey(autoGenerate = true) val id: Int,
                        val originalMarkId: Int?,
                        val type: Int,
                        val value: Float,
                        val weight: Float)