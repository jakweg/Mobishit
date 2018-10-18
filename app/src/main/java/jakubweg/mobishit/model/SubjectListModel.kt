package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.AverageCalculator
import jakubweg.mobishit.helper.MobiregPreferences

class SubjectListModel(application: Application)
    : BaseViewModel(application) {

    private val mSubjects = MutableLiveData<List<MarkDao.SubjectShortInfo>>()
    val subjects: LiveData<List<MarkDao.SubjectShortInfo>>
        get() =
        handleBackground(mSubjects).asImmutable

    override fun doInBackground() {
        val dao = AppDatabase.getAppDatabase(context).markDao

        val termId = MobiregPreferences.get(context).lastSelectedTerm

        mSubjects.postValue(dao.getSubjectsWithUsersMarks().apply {
            forEach {
                it.averageText = AverageCalculator.calculateAverage(context, termId, it.id).shortAverageText
            }
        })
    }
}