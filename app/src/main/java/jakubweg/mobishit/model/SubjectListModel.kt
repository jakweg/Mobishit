package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.db.LastMarkCacheData
import jakubweg.mobishit.helper.AverageCalculator
import jakubweg.mobishit.helper.MobiregPreferences

class SubjectListModel(application: Application)
    : BaseViewModel(application) {


    private val mLastMarks = MutableLiveData<List<LastMarkCacheData>>()
    private var lastMarksTask: LoadLastMarksTask? = null
    val lastMarks
        get() =
            mLastMarks.asImmutable.also {
                lastMarksTask.also { task ->
                    if (task == null) {
                        lastMarksTask = LoadLastMarksTask(this).apply { execute() }
                    }
                }
            }

    private var isShowingMoreMarks = false
    fun onClickedExpand() {
        if (isShowingMoreMarks) {
            mLastMarks.postValue(mLastMarks.value?.take(3))
            isShowingMoreMarks = false
            return
        }
        isShowingMoreMarks = true
        lastMarksTask?.cancel(false)
        lastMarksTask = LoadLastMarksTask(this).apply { execute() }
    }

    private class LoadLastMarksTask(private val model: SubjectListModel)
        : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {

            val count = if (model.isShowingMoreMarks) 12 else 3

            val markDao = AppDatabase.getAppDatabase(model.context)
                    .markDao
            val marks = if (!MobiregPreferences.get(null).hasReadyLastMarksCache) {
                MobiregPreferences.get(null).hasReadyLastMarksCache = true
                markDao.getLastMarks(12).also {
                    markDao.deleteCachedLastMarks()
                    markDao.insertLastMarks(it)
                }.take(count)
            } else markDao.getCachedLastMarks(count)
            model.mLastMarks.postValue(marks)
        }
    }


    private val mSubjects = MutableLiveData<List<AverageCacheData>>()
    val subjects: LiveData<List<AverageCacheData>>
        get() =
            handleBackground(mSubjects).asImmutable

    fun requestSubjectsAfterTermChanges() {
        if (lastTerm == MobiregPreferences.get(context).lastSelectedTerm)
            return
        cancelLastTask()
        handleBackground()
    }

    private var lastTerm = Int.MIN_VALUE
    var scrollPosition = 0

    override fun doInBackground() {
        val data = AverageCalculator
                .getOrCreateAverageCacheData(context)

        lastTerm = MobiregPreferences.get(context).lastSelectedTerm

        mSubjects.postValue(data)
    }
}