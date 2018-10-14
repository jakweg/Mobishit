package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao

class SubjectListModel(application: Application)
    : BaseViewModel(application) {

    private val mSubjects = MutableLiveData<List<MarkDao.SubjectShortInfo>>()
    val subjects: LiveData<List<MarkDao.SubjectShortInfo>>
        get() =
        handleBackground(mSubjects).asImmutable

    override fun doInBackground() {
        val dao = AppDatabase.getAppDatabase(context).markDao

        mSubjects.postValue(dao.getSubjectsWithUsersMarks())
    }
}