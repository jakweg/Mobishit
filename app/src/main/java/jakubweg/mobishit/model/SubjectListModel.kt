package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.helper.AverageCalculator
import jakubweg.mobishit.helper.MobiregPreferences

class SubjectListModel(application: Application)
    : BaseViewModel(application) {

    private val mSubjects = MutableLiveData<List<AverageCacheData>>()
    val subjects: LiveData<List<AverageCacheData>>
        get() =
        handleBackground(mSubjects).asImmutable

    fun requestSubjectsAfterTermChanges() {
        if (lastTerm == MobiregPreferences.get(context).lastSelectedTerm)
            return
        cancelLastTask()
        MobiregPreferences.get(context).hasReadyAverageCache = false
        handleBackground()
    }

    private var lastTerm = Int.MIN_VALUE

    override fun doInBackground() {
        val data = AverageCalculator
                .getOrCreateAverageCacheData(context)

        lastTerm = MobiregPreferences.get(context).lastSelectedTerm

        mSubjects.postValue(data)
    }
}