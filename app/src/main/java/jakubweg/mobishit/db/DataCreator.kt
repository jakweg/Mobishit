package jakubweg.mobishit.db

import android.graphics.Color
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import jakubweg.mobishit.helper.DateHelper

class DataCreator {
    class ObjectDeletedNotifier(
            val item: Any,
            val objectId: Int) : Exception()

    companion object {
        fun escapeString(string: String): String {
            if (!string.contains("\\\""))
                return string

            return string.replace("\\\"", "\"")
        }

        fun teacher(jr: JsonReader): Teacher {
            var id = 0
            var name = ""
            var surname = ""
            var login = ""
            var sex = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "surname" -> surname = jr.nextString()!!
                    "login" -> login = jr.nextString()!!
                    "sex" -> sex = jr.nextStringOrNull() ?: ""
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return Teacher(id, name, surname, login, sex).also {
                if (isDeleted)
                    throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun roomData(jr: JsonReader): RoomData {
            var id = 0
            var name = ""
            var description = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "description" -> description = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return RoomData(id, name, description).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun termData(jr: JsonReader): TermData {
            var id = 0
            var name = ""
            var type = ""
            var startDate = ""
            var endDate = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "type" -> type = jr.nextString()!!
                    "start_date" -> startDate = jr.nextString()!!
                    "end_date" -> endDate = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return TermData(id, name, type, DateHelper.stringDateToMillis(startDate), DateHelper.stringDateToMillis(endDate)).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun subjectData(jr: JsonReader): SubjectData {
            var id = 0
            var name = ""
            var abbr = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbr" -> abbr = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return SubjectData(id, name, abbr).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun groupData(jr: JsonReader): GroupData {
            var id = 0
            var name = ""
            var parentId = 0
            var type = ""
            var abbr = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "parent_id" -> parentId = jr.nextInt()
                    "abbr" -> abbr = jr.nextString()!!
                    "type" -> type = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return GroupData(id, name, parentId, type, abbr).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun groupTerm(jr: JsonReader): GroupTerm {
            var id = 0
            var groupsId = 0
            var termId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "groups_id" -> groupsId = jr.nextInt()
                    "terms_id" -> termId = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return GroupTerm(id, groupsId, termId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markScaleGroup(jr: JsonReader): MarkScaleGroup {
            var id = 0
            var name = ""
            var isPublic = false
            var markType = ""
            var isDefault = false
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "public" -> isPublic = jr.nextInt() != 0
                    "mark_types" -> markType = jr.nextString()!!
                    "is_default" -> isDefault = jr.nextInt() != 0
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkScaleGroup(id, name, markType, isPublic, isDefault).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markScale(jr: JsonReader): MarkScale {
            var id = 0
            var markScaleGroupId = 0
            var abbr = ""
            var name = ""
            var markValue = 0f
            var noCountToAverage = false
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "mark_scale_groups_id" -> markScaleGroupId = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbreviation" -> abbr = jr.nextString()!!
                    "mark_value" -> markValue = jr.nextDouble().toFloat()
                    "no_count_to_average" -> noCountToAverage = jr.nextInt() != 0
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkScale(id, markScaleGroupId, abbr, name, markValue, noCountToAverage).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markDivisionGroup(jr: JsonReader): MarkDivisionGroup {
            var id = 0
            var name = ""
            var type = 0
            var rangeMin = 0f
            var rangeMax = 0f
            var isPublic = false
            var markScaleGroupId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "public" -> isPublic = jr.nextInt() != 0
                    "mark_scale_groups_id" -> markScaleGroupId = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "type" -> type = jr.nextInt()
                    "range_min" -> rangeMin = jr.nextDouble().toFloat()
                    "range_max" -> rangeMax = jr.nextDouble().toFloat()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkDivisionGroup(id, name, type, isPublic, rangeMin, rangeMax, markScaleGroupId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markKind(jr: JsonReader): MarkKind {
            var id = 0
            var name = ""
            var abbreviation = ""
            var defaultMarkType: Int? = null
            var defaultMarkScaleGroupId: Int? = null
            var defaultWeight: Int? = null
            var position = 0
            var cssStyle = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbreviation" -> abbreviation = jr.nextString()!!
                    "default_mark_type" -> defaultMarkType = jr.nextIntOrNull()
                    "default_mark_scale_groups_id" -> defaultMarkScaleGroupId = jr.nextIntOrNull()
                    "default_weigth" -> defaultWeight = jr.nextIntOrNull()
                    "position" -> position = jr.nextInt()
                    "css_style" -> cssStyle = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkKind(id, name, abbreviation, defaultMarkType, defaultMarkScaleGroupId, defaultWeight, position, cssStyle).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markGroupGroup(jr: JsonReader): MarkGroupGroup {
            var id = 0
            var name = ""
            var position = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "position" -> position = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkGroupGroup(id, name, position).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markGroup(jr: JsonReader): MarkGroup {
            var id = 0
            var markKindId = 0
            var markScaleGroupId: Int? = null
            var eventTypeTermId = 0
            var weight: Float? = null
            var abbreviation = ""
            var description = ""
            var markType = 0
            var position = 0
            var countPointsWithoutBase = false
            var markValueMin: Int? = null
            var markValueMax: Int? = null
            var parentId: Int? = null
            var parentType: Int? = null
            var visibility: Int? = null
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "position" -> position = jr.nextInt()
                    "mark_kinds_id" -> markKindId = jr.nextInt()
                    "mark_scale_groups_id" -> markScaleGroupId = jr.nextIntOrNull()
                    "event_type_terms_id" -> eventTypeTermId = jr.nextInt()
                    "weight" -> weight = jr.nextFloatOrNull()
                    "abbreviation" -> abbreviation = jr.nextString()!!
                    "description" -> description = escapeString(jr.nextString()!!)
                    "mark_type" -> markType = jr.nextInt()
                    "count_points_without_base" -> countPointsWithoutBase = jr.nextInt() != 0
                    "parent_id" -> parentId = jr.nextIntOrNull()
                    "visibility" -> visibility = jr.nextInt()
                    "parent_type" -> parentType = jr.nextIntOrNull()
                    "mark_value_range_min" -> markValueMin = jr.nextIntOrNull()
                    "mark_value_range_max" -> markValueMax = jr.nextIntOrNull()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkGroup(id, markKindId, weight, markScaleGroupId, eventTypeTermId, abbreviation,
                    description, markType, position, countPointsWithoutBase, markValueMin, markValueMax,
                    parentId, parentType, visibility).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventType(jr: JsonReader): EventType {
            var id = 0
            var subjectId: Int? = null
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "subjects_id" -> subjectId = jr.nextIntOrNull()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventType(id, subjectId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventTypeTeacher(jr: JsonReader): EventTypeTeacher {
            var id = 0
            var teacherId = 0
            var eventTypeId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "teachers_id" -> teacherId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventTypeTeacher(id, teacherId, eventTypeId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventTypeTerm(jr: JsonReader): EventTypeTerm {
            var id = 0
            var termId = 0
            var eventTypeId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "terms_id" -> termId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventTypeTerm(id, termId, eventTypeId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventTypeGroup(jr: JsonReader): EventTypeGroup {
            var id = 0
            var groupId = 0
            var eventTypeId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "groups_id" -> groupId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventTypeGroup(id, groupId, eventTypeId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventData(jr: JsonReader): EventData {
            var id = 0
            var name: String? = null
            var date = ""
            var number: Int? = null
            var startTime = ""
            var endTime = ""
            var roomId: Int? = null
            var eventTypeId = 0
            var status = EventDao.STATUS_SCHEDULED
            var substitution = EventDao.SUBSTITUTION_NONE
            var termId = 0
            var lessonGroupId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextStringOrNull()
                    "date" -> date = jr.nextString()!!
                    "start_time" -> startTime = jr.nextString()!!
                    "end_time" -> endTime = jr.nextString()!!
                    "number" -> number = jr.nextIntOrNull()
                    "rooms_id" -> roomId = jr.nextIntOrNull()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "status" -> status = jr.nextInt()
                    "terms_id" -> termId = jr.nextInt()
                    "lesson_groups_id" -> lessonGroupId = jr.nextInt()
                    "substitution" -> substitution = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventData(id, name, DateHelper.stringDateToMillis(date), number, normalizeTime(startTime),
                    normalizeTime(endTime), roomId, eventTypeId, status, substitution, termId, lessonGroupId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }

        private fun normalizeTime(time: String): String {
            return if (time.length == 8)
                time.substring(0, 5) else time
        }


        fun eventIssue(jr: JsonReader): EventIssue {
            var id = 0
            var eventId = 0
            var issueId = 0
            var eventTypeId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "events_id" -> eventId = jr.nextInt()
                    "issues_id" -> issueId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventIssue(id, eventId, issueId, eventTypeId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventEvent(jr: JsonReader): EventEvent {
            var id = 0
            var event1Id = 0
            var event2Id = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "events1_id" -> event1Id = jr.nextInt()
                    "events2_id" -> event2Id = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventEvent(id, event1Id, event2Id).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun attendanceType(jr: JsonReader): AttendanceType {
            var id = 0
            var name = ""
            var abbreviation = ""
            var style = ""
            var countAs = ""
            var type = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbr" -> abbreviation = jr.nextString()!!
                    "style" -> style = jr.nextString()!!
                    "count_as" -> countAs = jr.nextString()!!
                    "type" -> type = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return AttendanceType(id, name, abbreviation, getStyleColor(style), countAs, type).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }

        private val regex = Regex(".*background-color:[ ]?#([a-fA-F0-9]{6}).*")

        private fun getStyleColor(style: String): Int {
            return Color.parseColor(
                    "#${regex.matchEntire(style)
                            ?.groupValues
                            ?.lastOrNull()
                            ?: "000000"}")
        }


        fun attendanceData(jr: JsonReader): AttendanceData {
            var id = 0
            var eventId = 0
            var typeId = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "events_id" -> eventId = jr.nextInt()
                    "types_id" -> typeId = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return AttendanceData(id, eventId, typeId).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun markData(jr: JsonReader): MarkData {
            var id = 0
            var markGroupId = 0
            var markScaleId: Int? = null
            var markValue: Float? = null
            var teacherId = 0
            var getDate = ""
            var addTime = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "mark_groups_id" -> markGroupId = jr.nextInt()
                    "mark_scales_id" -> markScaleId = jr.nextIntOrNull()
                    "mark_value" -> markValue = jr.nextFloatOrNull()
                    "teacher_users_id" -> teacherId = jr.nextInt()
                    "get_date" -> getDate = jr.nextString()!!
                    "add_time" -> addTime = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MarkData(id, markGroupId, markScaleId, teacherId, markValue, DateHelper.stringDateToMillis(getDate), DateHelper.stringTimeToMillis(addTime)).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun userReprimand(jr: JsonReader): MessageData {
            var id = 0
            var teacherId = 0
            var kindId = 0
            var addTime = ""
            var content = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "teachers_id" -> teacherId = jr.nextInt()
                    "kinds_id" -> kindId = jr.nextInt()
                    "addtime" -> addTime = jr.nextString()!!
                    "content" -> content = escapeString(jr.nextString()!!)
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MessageData(1000_000 + id, kindId, DateHelper.stringTimeToMillis(addTime),
                    teacherId, null, content, 0L).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun studentGroup(jr: JsonReader): StudentGroup {
            var id = 0
            var groupId = 0
            var number = 0
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "groups_id" -> groupId = jr.nextInt()
                    "number" -> number = jr.nextInt()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return StudentGroup(id, groupId, number).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun eventTypeSchedule(jr: JsonReader): EventTypeSchedule {
            var id = 0
            var eventTypeId = 0
            var scheduleId = 0
            var name = ""
            var number = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "schedules_id" -> scheduleId = jr.nextInt()
                    "number" -> number = jr.nextString()!!
                    "name" -> name = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return EventTypeSchedule(id, eventTypeId, scheduleId, name, number).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun lessonData(jr: JsonReader): LessonData {
            var id = 0
            var lessonNumber = 0
            var startTime = ""
            var endTime = ""
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "lesson_number" -> lessonNumber = jr.nextInt()
                    "start_time" -> startTime = jr.nextString()!!
                    "end_time" -> endTime = jr.nextString()!!
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return LessonData(id, lessonNumber, startTime, endTime).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


        fun messageData(jr: JsonReader): MessageData {
            var id = 0
            var sendTime = ""
            var senderId = 0
            var title = ""
            var content = ""
            var readTime: String? = null
            jr.beginObject()
            var isDeleted = false
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "send_time" -> sendTime = jr.nextString()!!
                    "sender_users_id" -> senderId = jr.nextInt()
                    "title" -> title = escapeString(jr.nextString()!!)
                    "content" -> content = escapeString(jr.nextString()!!)
                    "read_time" -> readTime = jr.nextStringOrNull()
                    "action" -> isDeleted = jr.nextString() == "D"
                    else -> jr.skipValue()
                }
            }

            return MessageData(id, MessageDao.KIND_JUST_MESSAGE,
                    DateHelper.stringTimeToMillis(sendTime), senderId,
                    title, content, DateHelper.stringTimeToMillis(readTime)).also {
                if (isDeleted) throw ObjectDeletedNotifier(it, it.id)
            }
        }


    }

}

fun JsonReader.nextStringOrNull() = if (peek() == JsonToken.NULL) {
    nextNull(); null
} else nextString()

fun JsonReader.nextIntOrNull() = if (peek() == JsonToken.NULL) {
    nextNull(); null
} else nextInt()

fun JsonReader.nextFloatOrNull() = if (peek() == JsonToken.NULL) {
    nextNull(); null
} else nextDouble().toFloat()

val JsonElement?.asStringOrNull get() = if (this == null || isJsonNull) null else asString!!

fun JsonObject.getBoolean(key: String, defaultValue: Boolean): Boolean {
    val obj = this[key] ?: return defaultValue
    return if (obj.isJsonPrimitive)
        obj.asBoolean
    else
        obj.asString?.equals("true", true) ?: defaultValue
}
