package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.EventDao

class OneDayModel(application: Application) : BaseViewModel(application) {

    private var mDay: Long = 0
    fun init(day: Long) {
        if (day == mDay) return
        check(mDay == 0L)
        mDay = day
    }

    private val mEvents = MutableLiveData<Array<EventDao.EventLongInfo>>()

    val events get() = handleBackground(mEvents).asImmutable

    override fun doInBackground() {
        val day = mDay

        val dao = AppDatabase.getAppDatabase(context).eventDao
        val events = dao.getAllEventsByDay(day).toTypedArray()
        mEvents.postValue(events)
    }
}