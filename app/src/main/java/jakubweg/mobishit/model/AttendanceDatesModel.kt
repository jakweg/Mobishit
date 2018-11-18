package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.helper.DateHelper
import java.util.*
import kotlin.collections.ArrayList

class AttendanceDatesModel(application: Application)
    : BaseViewModel(application) {

    private var start = 0L
    private var end = 0L
    private var subjectId = 0
    private var attendanceType = 0
    fun init(start: Long, end: Long, subjectId: Int, attendanceType: Int) {
        this.start = start
        this.end = end
        this.subjectId = subjectId
        this.attendanceType = attendanceType
    }

    class AttendanceDayItem(val isDayElement: Boolean,
                            val title: String,
                            val date: Long)

    private val mDays = MutableLiveData<List<AttendanceDayItem>>()
    val days
        get() = handleBackground(mDays).asImmutable


    override fun doInBackground() {
        val dao = AppDatabase.getAppDatabase(context).attendanceDao

        val days = when (subjectId) {
            -1 -> dao.getAttendanceDatesByAnySubject(start, end, attendanceType)
            0 -> dao.getAttendanceDatesByNullSubject(start, end, attendanceType)
            else -> dao.getAttendanceDatesBySubject(start, end, subjectId, attendanceType)
        }

        val outputDays = ArrayList<AttendanceDayItem>(days.size)

        val calendar = Calendar.getInstance()!!

        var previousDay = -1
        days.forEach { item ->
            calendar.timeInMillis = item.date
            val day = calendar[Calendar.DAY_OF_YEAR]
            if (previousDay != day) {
                previousDay = day
                val dayName = DateHelper.weekDaysMap[calendar[Calendar.DAY_OF_WEEK]]!!
                outputDays.add(AttendanceDayItem(true,
                        "$dayName • ${DateHelper.millisToStringDate(item.date)}", item.date))
            }

            val title = item.title?.takeUnless { it.isBlank() } ?: "Wydarzenie bez nazwy"

            outputDays.add(AttendanceDayItem(false,
                    if (item.number < 0) title else "${item.number} • $title", item.date))
        }

        mDays.postValue(outputDays)
    }
}
