package jakubweg.mobishit.db

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName


@Entity(tableName = "Teachers")
class Teacher(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val surname: String, val login: String, val sex: String)

@Entity(tableName = "Rooms")
class RoomData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val description: String)

@Entity(tableName = "Terms")
class TermData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val type: String, val startDate: Long, val endDate: Long)

@Entity(tableName = "Subjects")
class SubjectData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, @SerializedName("abbr") val abbreviation: String)

@Entity(tableName = "Groups")
class GroupData(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val parentId: Int, val type: String, @SerializedName("abbr") val abbreviation: String)

@Entity(tableName = "GroupsTerms")
class GroupTerm(@PrimaryKey(autoGenerate = true) val id: Int, val groupId: Int, val termId: Int)

@Entity(tableName = "MarkScaleGroups")
class MarkScaleGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String)

@Entity(tableName = "MarkScales")
class MarkScale(@PrimaryKey(autoGenerate = true) val id: Int, val markScaleGroupId: Int, val abbreviation: String, val name: String, val markValue: Float, val noCountToAverage: Boolean)

@Entity(tableName = "MarkDivisionGroups")
class MarkDivisionGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val type: Int, val rangeMin: Float, val rangeMax: Float, val markScaleGroupsId: Int)

@Entity(tableName = "MarkKinds")
class MarkKind(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val abbreviation: String, val defaultMarkType: Int?, val defaultMarkScaleGroupId: Int?, val defaultWeight: Int?, val position: Int, val cssStyle: String)

@Entity(tableName = "MarkGroupGroups")
class MarkGroupGroup(@PrimaryKey(autoGenerate = true) val id: Int, val name: String, val position: Int)

@Entity(tableName = "MarkGroups")
class MarkGroup(@PrimaryKey(autoGenerate = true) val id: Int, val markKindId: Int, val markScaleGroupId: Int?, val eventTypeTermId: Int, val abbreviation: String, val description: String, val markType: Int, val position: Int, val countPointsWithoutBase: Boolean, val markValueMin: Int?, val markValueMax: Int?, val parentId: Int?, val parentType: Int?, val visibility: Int?)

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
class AverageCacheData(@PrimaryKey(autoGenerate = true) val id: Int, val subjectId: Int, val subjectName: String?, var marks: String?, var weightedAverage: Float, var gotPointsSum: Float, var baseSum: Float) {

    fun getMarksList(): List<String> {
        return marks?.split('@') ?: emptyList()
    }


    fun setMarksList(list: List<MarkDao.MarkShortInfo>?) {
        marks = list?.joinToString(separator = "@") {
            when {
                it.markScaleValue >= 0 -> it.abbreviation ?: ""
                it.markPointsValue >= 0 -> it.markPointsValue.toString()
                else -> ""
            }
        }
    }

    private inline val Float.str1 get() = String.format("%.1f", this)
    private inline val Float.str2 get() = String.format("%.2f", this)

    private fun buildAverageText(): String {
        val hasPoints = gotPointsSum > 0f || baseSum > 0f
        val hasWeightedAverage = weightedAverage > 0f
        return when {
            hasPoints && hasWeightedAverage ->
                "Średnia: ${weightedAverage.str2}\n" +
                        "Zdobyte punkty: ${gotPointsSum.str1} na ${baseSum.str1} " +
                        "czyli ${(gotPointsSum / baseSum * 100f).toInt()}%"

            hasPoints ->
                "Zdobyte punkty: ${gotPointsSum.str1} na ${baseSum.str1} " +
                        "czyli ${(gotPointsSum / baseSum * 100f).toInt()}%"

            hasWeightedAverage ->
                "Twoja średnia ważona wynosi ${weightedAverage.str2}"

            else -> "Brak danych"
        }.also { _averageText = it }
    }

    @Ignore
    private var _averageText: String? = null
    val averageText get() = _averageText ?: buildAverageText()


    private fun buildShortAverageText(): String {
        val hasPoints = gotPointsSum > 0f || baseSum > 0f
        val hasWeightedAverage = weightedAverage > 0f
        return when {
            hasPoints && hasWeightedAverage ->
                "${weightedAverage.str2}\n" +
                        "${gotPointsSum.str1}/${baseSum.str1} ${(gotPointsSum / baseSum * 100f).toInt()}%"

            hasPoints ->
                "${gotPointsSum.str1}/${baseSum.str1}\n${(gotPointsSum / baseSum * 100f).toInt()}%"

            hasWeightedAverage -> weightedAverage.str2

            else -> ""
        }.also { _shortAverageText = it }
    }

    @Ignore
    private var _shortAverageText: String? = null
    val shortAverageText get() = _shortAverageText ?: buildShortAverageText()

}
