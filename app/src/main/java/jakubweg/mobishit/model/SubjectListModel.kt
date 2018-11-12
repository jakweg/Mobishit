package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.helper.AverageCalculator

class SubjectListModel(application: Application)
    : BaseViewModel(application) {

    private val mSubjects = MutableLiveData<List<AverageCacheData>>()
    val subjects: LiveData<List<AverageCacheData>>
        get() =
        handleBackground(mSubjects).asImmutable

    override fun doInBackground() {
        mSubjects.postValue(AverageCalculator.getOrCreateAverageCacheData(context))
    }
}