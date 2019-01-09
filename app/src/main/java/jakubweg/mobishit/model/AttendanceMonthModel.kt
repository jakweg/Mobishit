package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.AttendanceDao

class AttendanceMonthModel(application: Application)
    : BaseViewModel(application) {


    private var mStart = -1L
    private var mEnd = -1L
    private var mSubjectId = -1
    fun init(start: Long, end: Long, subjectId: Int) {
        mStart = start
        mEnd = end
        mSubjectId = subjectId
    }


    // name, color, count
    private val mAttendanceTypes = MutableLiveData<List<AttendanceDao.AttendanceTypeAndCountInfo>>()

    val attendanceTypes
        get() = handleBackground(mAttendanceTypes).asImmutable


    override fun doInBackground() {
        require(mStart >= 0L && mEnd >= 0L)
        val dao = AppDatabase.getAppDatabase(context).attendanceDao

        val attendances =
                when (mSubjectId) {
                    -1 -> dao.getDetailedAttendancesBetweenDates(mStart, mEnd)
                    else -> dao.getDetailedAttendancesBetweenDates(mStart, mEnd, mSubjectId)
                }

        mAttendanceTypes.postValue(attendances
                .sortedByDescending { it.count })
    }
}
