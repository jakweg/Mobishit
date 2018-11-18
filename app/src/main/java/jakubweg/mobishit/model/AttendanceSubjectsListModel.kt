package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.AttendanceDao

class AttendanceSubjectsListModel(application: Application)
    : BaseViewModel(application) {

    private var mStart = 0L
    private var mEnd = 0L

    fun init(start: Long, end: Long) {
        mStart = start
        mEnd = end
    }

    private val mSubjects = MutableLiveData<List<AttendanceDao.AttendanceSubjectInfo>>()
    val subjects
        get() = handleBackground(mSubjects).asImmutable

    override fun doInBackground() {
        require(mStart > 0 && mEnd > 0)

        val dao = AppDatabase.getAppDatabase(context).attendanceDao

        mSubjects.postValue(dao.getAttendanceSubjectInfoBetweenDates(mStart, mEnd).sortedByDescending { it.percentage })
    }
}
