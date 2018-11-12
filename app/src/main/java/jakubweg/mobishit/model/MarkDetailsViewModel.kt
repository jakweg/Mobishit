package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao

class MarkDetailsViewModel(application: Application) : BaseViewModel(application) {
    private var markId = 0
    fun init(markId: Int) {
        require(markId != 0)
        this.markId = markId
        cancelLastTask()
    }

    private val mMark = MutableLiveData<MarkDao.MarkDetails>()
    val mark get() = handleBackground(mMark).asImmutable

    override fun doInBackground() {
        if (markId == 0) cancelLastTask()
        val dao = AppDatabase.getAppDatabase(context).markDao

        mMark.postValue(dao.getMarkDetails(markId))
    }
}