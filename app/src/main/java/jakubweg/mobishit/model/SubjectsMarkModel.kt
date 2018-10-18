package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.support.v4.util.ArrayMap
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.AverageCalculator
import jakubweg.mobishit.helper.MobiregPreferences

class SubjectsMarkModel(application: Application)
    : BaseViewModel(application) {

    private var mSubjectId = 0
    fun init(subjectId: Int) {
        require(subjectId != 0) { "Subject id must be not equal to 0" }
        if (mSubjectId == subjectId) return
        check(mSubjectId == 0) { "SubjectsMarkModel.init was already called!" }
        mSubjectId = subjectId
    }

    private var preferences: MobiregPreferences? = null
    private var mSelectedTermId = 0

    var selectedTermId
        get() = mSelectedTermId
        set(value) {
            mSelectedTermId = value
            preferences?.lastSelectedTerm = value
        }

    private val mTerms = MutableLiveData<List<MarkDao.TermShortInfo>>()

    val terms get() = handleBackground(mTerms).asImmutable

    private val mMarks = MutableLiveData<ArrayMap<Int, List<MarkDao.MarkShortInfo>>>()

    val marks get() = handleBackground(mMarks).asImmutable

    private val mAverages = MutableLiveData<ArrayMap<Int, AverageCalculator.AverageCalculationResult>>()

    val averages get() = handleBackground(mAverages).asImmutable

    override fun doInBackground() {
        check(mSubjectId != 0) { "SubjectsMarkModel.init not called!" }

        preferences = MobiregPreferences.get(context)

        val (terms, marksMap, averagesMap) = AverageCalculator.getMarksAndCalculateAverage(context, mSubjectId)

        mSelectedTermId = preferences?.lastSelectedTerm ?: 0
        mAverages.postValue(averagesMap)
        mMarks.postValue(marksMap)
        mTerms.postValue(terms)
    }
}