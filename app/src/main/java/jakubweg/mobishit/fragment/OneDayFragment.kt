package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jakubweg.mobishit.R
import jakubweg.mobishit.db.EventDao
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.OneDayModel
import java.lang.ref.WeakReference


class OneDayFragment : Fragment() {
    companion object {
        private var lastListViewCreationTime = 0L

        fun newInstance(millis: Long) = OneDayFragment().apply { arguments = Bundle().also { it.putLong("millis", millis) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_one_day_timetable, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val millis = arguments!!.getLong("millis", -1L)

        val eventsList = view.findViewById<RecyclerView>(R.id.eventsList)
        eventsList.addItemDecoration(DividerItemDecoration(eventsList.context, DividerItemDecoration.VERTICAL))

        val model = ViewModelProviders.of(this)[OneDayModel::class.java]

        model.init(millis)

        model.events.observe(this, SafeObserver(eventsList))
        if (eventsList.adapter == null)
            eventsList.adapter = EmptyAdapter("Ładowanie danych...")
    }

    private class SafeObserver(eventsListStrong: RecyclerView)
        : Observer<Array<EventDao.EventLongInfo>> {
        private val eventsList = WeakReference<RecyclerView>(eventsListStrong)
        override fun onChanged(lessons: Array<EventDao.EventLongInfo>?) {
            eventsList.get()?.also { view->
                view.postDelayed({
                    requestAdapterCreation(lessons)
                }, 350L)
            }
        }

        private fun requestAdapterCreation(events: Array<EventDao.EventLongInfo>?) {
            val eventsList = eventsList.get() ?: return
            if (lastListViewCreationTime + 350L > System.currentTimeMillis()) {
                eventsList.postDelayed({ requestAdapterCreation(events) }, 350L)
            } else {
                lastListViewCreationTime = System.currentTimeMillis()
                if (events?.isNotEmpty() == true)
                    eventsList.adapter = Adapter(eventsList.context ?: return, events)
                else
                    eventsList.adapter = EmptyAdapter("Brak lekcji w ten dzień")
            }
        }
    }

    private class Adapter(context: Context,
                          val lessons: Array<EventDao.EventLongInfo>)
        : RecyclerView.Adapter<Adapter.BaseViewHolder>() {

        companion object {
            private const val TYPE_NORMAL = 0
            private const val TYPE_CANCELLED = 1
            private const val TYPE_GAP = 2
        }

        private val inflater = LayoutInflater.from(context)!!
        private val showLessonNumber = MobiregPreferences.get(context).showLessonNumberOnTimetable
        private val gapListPositions: List<Int>

        init{
            if(MobiregPreferences.get(context).showGapsBetweenLessons) {
                val gaps = mutableListOf<Int>()
                var listPosition = 0
                for ((i, lesson) in lessons.withIndex()) {
                    var addGap = false
                    if (lesson.number != null) {
                        if (i == 0) {
                            if (lesson.number > 1)
                                addGap = true
                        } else {
                            val prevLesson = lessons[i - 1]
                            if (prevLesson.number != null && lesson.number > prevLesson.number + 1)
                                addGap = true
                        }
                    }

                    if (addGap)
                        gaps.add(listPosition++)
                    listPosition++
                }
                gapListPositions = gaps
            } else{
                gapListPositions = emptyList()
            }
        }

        override fun getItemCount() = lessons.size + gapListPositions.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : BaseViewHolder = when (viewType) {
            TYPE_NORMAL -> NormalLessonViewHolder(inflater.inflate(R.layout.lesson_list_item, parent, false), showLessonNumber)
            TYPE_CANCELLED -> CancelledLessonViewHolder(inflater.inflate(R.layout.cancelled_lesson_list_item, parent, false))
            TYPE_GAP -> GapBetweenLessonsViewHolder(inflater.inflate(R.layout.gap_between_lessons_list_item, parent, false))
            else -> throw IllegalArgumentException()
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: BaseViewHolder, pos: Int) {
            holder.bindSelf(if(pos in gapListPositions) null else lessons[getLessonIndexFromListPosition(pos)])
        }

        override fun getItemViewType(position: Int): Int {
            if(position in gapListPositions)
                return TYPE_GAP

            return lessons[getLessonIndexFromListPosition(position)].run {
                when (status) {
                    EventDao.STATUS_SCHEDULED -> {
                        when (this.substitution) {
                            EventDao.SUBSTITUTION_OLD_LESSON -> TYPE_CANCELLED
                            EventDao.SUBSTITUTION_NONE, EventDao.SUBSTITUTION_NEW_LESSON -> TYPE_NORMAL
                            else -> {
                                Log.e("OneDayFragment\$Adapter", "Unknown substitution $substitution")
                                TYPE_NORMAL
                            }
                        }
                    }
                    EventDao.STATUS_COMPLETED -> TYPE_NORMAL
                    EventDao.STATUS_CANCELED -> TYPE_CANCELLED
                    else -> {
                        Log.e("OneDayFragment\$Adapter", "Unknown status $status")
                        TYPE_NORMAL
                    }
                }
            }
        }

        private fun getLessonIndexFromListPosition(position: Int) =
            position - gapListPositions.count {it < position}

        private abstract class BaseViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            abstract fun bindSelf(eventInfo: EventDao.EventLongInfo?)
        }

        private class NormalLessonViewHolder(v: View,
                                             private val showLessonNumber: Boolean) : BaseViewHolder(v) {
            private val mainText = v.textView(R.id.mainText)!!
            private val colorView = v.findViewById<View>(R.id.eventColorView)!!
            private val secondaryText = v.textView(R.id.secondaryText)!!
            private val hoursText = v.textView(R.id.hoursText)!!
            private val lessonNumber = v.textView(R.id.lessonNumber)

            @SuppressLint("SetTextI18n")
            override fun bindSelf(eventInfo: EventDao.EventLongInfo?) {
                eventInfo!!
                eventInfo.apply {
                    colorView.setBackgroundColor(color ?: Color.LTGRAY)
                    mainText.precomputedText = subjectName.takeIfNotBlankOrNull() ?: description.takeIfNotBlankOrNull() ?: "Wydarzenie bez nazwy"

                    val hasRoom = roomName != null
                    val hasTeacher = teacherName != null
                    val isSubstitution = substitution == EventDao.SUBSTITUTION_NEW_LESSON
                    val hasAttendance = attendanceName != null

                    secondaryText.text = buildString {
                        when {
                            hasTeacher && hasRoom -> "$teacherName • $roomName"
                            hasTeacher -> teacherName
                            hasRoom -> roomName
                            else -> null
                        }?.also {
                            append(it)
                        }

                        when {
                            isSubstitution && hasAttendance -> "Zastępstwo • $attendanceName"
                            isSubstitution -> "Zastępstwo"
                            hasAttendance -> attendanceName
                            else -> null
                        }?.also {
                            append('\n')
                            append(it)
                        }
                    }

                    hoursText.precomputedText = "${normalizeHour(startTime)}\n${normalizeHour(endTime)}"
                    if (showLessonNumber)
                        lessonNumber?.precomputedText = number?.toString() ?: "-"
                    else
                        lessonNumber?.visibility = View.GONE
                }
            }

            private fun normalizeHour(hour: String) = if (hour.length == 8) hour.substring(0, 5) else hour

            private fun String?.takeIfNotBlankOrNull(): String? {
                this ?: return null
                if (isBlank())
                    return null
                return this
            }
        }

        private class CancelledLessonViewHolder(v: View) : BaseViewHolder(v) {
            private val mainText = v.textView(R.id.mainText)!!
            private val secondaryText = v.textView(R.id.secondaryText)!!

            init {
                mainText.paintFlags = mainText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }

            override fun bindSelf(eventInfo: EventDao.EventLongInfo?) {
                eventInfo!!
                eventInfo.apply {
                    mainText.precomputedText = subjectName ?: "Brak nazwy przedmiotu"
                    secondaryText.precomputedText = description?.takeUnless { it.isBlank() } ?: when (substitution) {
                        EventDao.SUBSTITUTION_OLD_LESSON -> "Lekcja zastąpiona"
                        else -> "Lekcja odwołana"
                    }
                }
            }
        }

        private class GapBetweenLessonsViewHolder(v: View): BaseViewHolder(v) {
            override fun bindSelf(eventInfo: EventDao.EventLongInfo?) = Unit
        }

    }
}