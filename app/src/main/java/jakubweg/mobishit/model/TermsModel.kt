package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.TermDao
import jakubweg.mobishit.helper.AverageCalculator
import jakubweg.mobishit.helper.MobiregPreferences

class TermsModel(application: Application) :
        BaseViewModel(application) {

    var selectedTermId = 0
    var selectedOrderMethod = AverageCalculator.ORDER_DEFAULT
    var isGroupingByParentsEnabled = true

    private val mTerms = MutableLiveData<List<TermDao.TermShortInfo>>()
    val terms get() = handleBackground(mTerms).asImmutable

    fun savePreferences() {
        MobiregPreferences.get(context).also {
            it.lastSelectedTerm = selectedTermId
            it.markSortingOrder = selectedOrderMethod
            it.groupMarksByParent = isGroupingByParentsEnabled
        }
    }

    override fun doInBackground() {
        MobiregPreferences.get(context).also {
            selectedTermId = it.lastSelectedTerm
            selectedOrderMethod = it.markSortingOrder
            isGroupingByParentsEnabled = it.groupMarksByParent
        }
        mTerms.postValue(AppDatabase.getAppDatabase(context)
                .termDao.getTermsShortInfo())
    }
}
