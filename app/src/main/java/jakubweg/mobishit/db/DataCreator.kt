package jakubweg.mobishit.db

import android.graphics.Color
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import jakubweg.mobishit.helper.ChangedObjectsLog
import jakubweg.mobishit.helper.DateHelper

class DataCreator {

    companion object {
        private fun escapeString(string: String): String {
            if (!string.contains("\\\""))
                return string

            return string.replace("\\\"", "\"")
        }

        fun teacher(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var surname = ""
            var login = ""
            var sex = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "surname" -> surname = jr.nextString()!!
                    "login" -> login = jr.nextString()!!
                    "sex" -> sex = jr.nextStringOrNull() ?: ""
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, Teacher(id, name, surname, login, sex))
        }


        fun roomData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var description = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "description" -> description = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, RoomData(id, name, description))
        }


        fun termData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var type = ""
            var startDate = ""
            var endDate = ""
            var parentId: Int? = null
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "type" -> type = jr.nextString()!!
                    "start_date" -> startDate = jr.nextString()!!
                    "end_date" -> endDate = jr.nextString()!!
                    "parent_id" -> parentId = jr.nextIntOrNull()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, TermData(id, name, type, parentId,
                    DateHelper.stringDateToMillis(startDate), DateHelper.stringDateToMillis(endDate)))
        }


        fun subjectData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var abbr = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbr" -> abbr = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, SubjectData(id, name, abbr, false))
        }


        fun groupData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var parentId = 0
            var type = ""
            var abbr = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "parent_id" -> parentId = jr.nextInt()
                    "abbr" -> abbr = jr.nextString()!!
                    "type" -> type = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, GroupData(id, name, parentId, type, abbr))
        }


        fun groupTerm(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var groupsId = 0
            var termId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "groups_id" -> groupsId = jr.nextInt()
                    "terms_id" -> termId = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, GroupTerm(id, groupsId, termId))
        }


        fun markScaleGroup(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var isPublic = false
            var markType = ""
            var isDefault = false
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "public" -> isPublic = jr.nextInt() != 0
                    "mark_types" -> markType = jr.nextString()!!
                    "is_default" -> isDefault = jr.nextInt() != 0
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkScaleGroup(id, name, markType, isPublic, isDefault))
        }


        fun markScale(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var markScaleGroupId = 0
            var abbr = ""
            var name = ""
            var markValue = 0f
            var noCountToAverage = false
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "mark_scale_groups_id" -> markScaleGroupId = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbreviation" -> abbr = jr.nextString()!!
                    "mark_value" -> markValue = jr.nextDouble().toFloat()
                    "no_count_to_average" -> noCountToAverage = jr.nextInt() != 0
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkScale(id, markScaleGroupId, abbr, name, markValue, noCountToAverage))
        }


        fun markDivisionGroup(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var type = 0
            var rangeMin = 0f
            var rangeMax = 0f
            var isPublic = false
            var markScaleGroupId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "public" -> isPublic = jr.nextInt() != 0
                    "mark_scale_groups_id" -> markScaleGroupId = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "type" -> type = jr.nextInt()
                    "range_min" -> rangeMin = jr.nextDouble().toFloat()
                    "range_max" -> rangeMax = jr.nextDouble().toFloat()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkDivisionGroup(id, name, type, isPublic, rangeMin, rangeMax, markScaleGroupId))
        }


        fun markKind(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var abbreviation = ""
            var defaultMarkType: Int? = null
            var defaultMarkScaleGroupId: Int? = null
            var defaultWeight: Int? = null
            var position = 0
            var cssStyle = ""
            jr.beginObject()
            var action = ""
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
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkKind(id, name, abbreviation, defaultMarkType, defaultMarkScaleGroupId, defaultWeight, position, cssStyle))
        }


        fun markGroupGroup(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var position = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "position" -> position = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkGroupGroup(id, name, position))
        }


        fun markGroup(jr: JsonReader, log: ChangedObjectsLog) {
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
            var action = ""
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
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkGroup(id, markKindId, weight, markScaleGroupId, eventTypeTermId, abbreviation,
                    description, markType, position, countPointsWithoutBase, markValueMin, markValueMax,
                    parentId, parentType, visibility))
        }


        fun eventType(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var subjectId: Int? = null
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "subjects_id" -> subjectId = jr.nextIntOrNull()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventType(id, subjectId))
        }


        fun eventTypeTeacher(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var teacherId = 0
            var eventTypeId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "teachers_id" -> teacherId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventTypeTeacher(id, teacherId, eventTypeId))
        }


        fun eventTypeTerm(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var termId = 0
            var eventTypeId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "terms_id" -> termId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventTypeTerm(id, termId, eventTypeId))
        }


        fun eventTypeGroup(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var groupId = 0
            var eventTypeId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "groups_id" -> groupId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventTypeGroup(id, groupId, eventTypeId))
        }


        fun eventData(jr: JsonReader, log: ChangedObjectsLog) {
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
            var action = ""
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
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventData(id, name, DateHelper.stringDateToMillis(date), number, normalizeTime(startTime),
                    normalizeTime(endTime), roomId, eventTypeId, status, substitution, termId, lessonGroupId))
        }

        private fun normalizeTime(time: String): String {
            return if (time.length == 8)
                time.substring(0, 5) else time
        }


        fun eventIssue(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var eventId = 0
            var issueId = 0
            var eventTypeId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "events_id" -> eventId = jr.nextInt()
                    "issues_id" -> issueId = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventIssue(id, eventId, issueId, eventTypeId))
        }


        fun eventEvent(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var event1Id = 0
            var event2Id = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "events1_id" -> event1Id = jr.nextInt()
                    "events2_id" -> event2Id = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventEvent(id, event1Id, event2Id))
        }


        fun attendanceType(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var name = ""
            var abbreviation = ""
            var style = ""
            var countAs = ""
            var type = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "name" -> name = jr.nextString()!!
                    "abbr" -> abbreviation = jr.nextString()!!
                    "style" -> style = jr.nextString()!!
                    "count_as" -> countAs = jr.nextString()!!
                    "type" -> type = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, AttendanceType(id, name, abbreviation, getStyleColor(style), countAs, type))
        }

        private val regex = Regex(".*background-color:[ ]?#([a-fA-F0-9]{6}).*")

        private fun getStyleColor(style: String): Int {
            return Color.parseColor(
                    "#${regex.matchEntire(style)
                            ?.groupValues
                            ?.lastOrNull()
                            ?: "000000"}")
        }


        fun attendanceData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var eventId = 0
            var typeId = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "events_id" -> eventId = jr.nextInt()
                    "types_id" -> typeId = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, AttendanceData(id, eventId, typeId))
        }


        fun markData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var markGroupId = 0
            var markScaleId: Int? = null
            var markValue: Float? = null
            var teacherId = 0
            var getDate = ""
            var addTime = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "mark_groups_id" -> markGroupId = jr.nextInt()
                    "mark_scales_id" -> markScaleId = jr.nextIntOrNull()
                    "mark_value" -> markValue = jr.nextFloatOrNull()
                    "teacher_users_id" -> teacherId = jr.nextInt()
                    "get_date" -> getDate = jr.nextString()!!
                    "add_time" -> addTime = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MarkData(id, markGroupId, markScaleId, teacherId, markValue,
                    DateHelper.stringDateToMillis(getDate), DateHelper.stringTimeToMillis(addTime)))
        }


        fun userReprimand(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var teacherId = 0
            var kindId = 0
            var addTime = ""
            var content = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "teachers_id" -> teacherId = jr.nextInt()
                    "kinds_id" -> kindId = jr.nextInt()
                    "addtime" -> addTime = jr.nextString()!!
                    "content" -> content = escapeString(jr.nextString()!!)
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, MessageData(1000_000 + id, kindId, DateHelper.stringTimeToMillis(addTime),
                    teacherId, null, content, 0L))
        }


        fun studentGroup(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var groupId = 0
            var number = 0
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "groups_id" -> groupId = jr.nextInt()
                    "number" -> number = jr.nextInt()
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, StudentGroup(id, groupId, number))
        }


        fun eventTypeSchedule(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var eventTypeId = 0
            var scheduleId = 0
            var name = ""
            var number = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "event_types_id" -> eventTypeId = jr.nextInt()
                    "schedules_id" -> scheduleId = jr.nextInt()
                    "number" -> number = jr.nextString()!!
                    "name" -> name = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, EventTypeSchedule(id, eventTypeId, scheduleId, name, number))
        }


        fun lessonData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var lessonNumber = 0
            var startTime = ""
            var endTime = ""
            jr.beginObject()
            var action = ""
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "lesson_number" -> lessonNumber = jr.nextInt()
                    "start_time" -> startTime = jr.nextString()!!
                    "end_time" -> endTime = jr.nextString()!!
                    "action" -> action = jr.nextString()
                    else -> jr.skipValue()
                }
            }

            log.new(action, LessonData(id, lessonNumber, startTime, endTime))
        }


        fun messageData(jr: JsonReader, log: ChangedObjectsLog) {
            var id = 0
            var sendTime = ""
            var senderId = 0
            var title = ""
            var content = ""
            var readTime: String? = null
            jr.beginObject()
            while (jr.hasNext()) {
                when (jr.nextName()) {
                    "id" -> id = jr.nextInt()
                    "send_time" -> sendTime = jr.nextString()!!
                    "sender_users_id" -> senderId = jr.nextInt()
                    "title" -> title = escapeString(jr.nextString()!!)
                    "content" -> content = escapeString(jr.nextString()!!)
                    "read_time" -> readTime = jr.nextStringOrNull()
                    else -> jr.skipValue()
                }
            }

            // NOTE: messages may not have action parameter, so we use always 'I' instead!!
            log.new("I", MessageData(id, MessageDao.KIND_JUST_MESSAGE,
                    DateHelper.stringTimeToMillis(sendTime), senderId,
                    title, content, DateHelper.stringTimeToMillis(readTime)))
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
