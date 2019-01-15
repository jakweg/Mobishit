package jakubweg.mobishit.model

import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_POINTS_SINGLE
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_CHILD
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_PARENT
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_SINGLE
import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.db.TermDao
import jakubweg.mobishit.db.VirtualMarkEntity
import jakubweg.mobishit.fragment.AboutVirtualMarksFragment
import jakubweg.mobishit.helper.MobiregPreferences

@Suppress("NOTHING_TO_INLINE")
class AboutVirtualMarksModel(app: Application) : BaseViewModel(app) {

    companion object {
        const val REQUEST_NOTHING = 0
        const val REQUEST_GET_MARK_SCALE_GROUPS = 1
        const val REQUEST_GET_SUBJECTS = 2
        const val REQUEST_GET_MARK_SCALE_GROUPS_BY_SUBJECT = 3
        const val REQUEST_GET_TERMS = 4
        const val REQUEST_IMPORT_MARKS = 5
    }

    val currentRealizedRequest = MutableLiveData<Int>().apply { value = REQUEST_NOTHING }


    var markScaleGroups = listOf<MarkDao.MarkScaleGroupShortInfo>()
    var subjects = listOf<MarkDao.SubjectShortInfo>()
    var terms = listOf<TermDao.TermShortInfo>()

    var selectedSubjectsId = -1
    var selectedMarkScaleGroupsId = -1
    var selectedTermsId = -1

    private var currentRequestedState = REQUEST_NOTHING
    inline fun requestNothing() = request(REQUEST_NOTHING)

    inline fun requestMarkScaleGroups() = request(REQUEST_GET_MARK_SCALE_GROUPS)

    inline fun requestSubjects() = request(REQUEST_GET_SUBJECTS)

    inline fun requestScaleGroupsBySubject() = request(REQUEST_GET_MARK_SCALE_GROUPS_BY_SUBJECT)

    inline fun requestTerms() = request(REQUEST_GET_TERMS)

    inline fun requestImportingMarks() = request(REQUEST_IMPORT_MARKS)

    fun request(what: Int) {
        if (currentRequestedState == what)
            return
        currentRequestedState = what
        cancelLastTask()
        handleBackground()
    }

    override fun doInBackground() {
        val db = AppDatabase.getAppDatabase(context)
        val markDao = db.markDao
        when (currentRequestedState) {
            REQUEST_GET_MARK_SCALE_GROUPS -> {
                markScaleGroups = markDao.getUsedMarkScaleGroups()
            }
            REQUEST_GET_SUBJECTS -> {
                subjects = markDao.getSubjectsWithCountedUsersMarks()
            }
            REQUEST_GET_MARK_SCALE_GROUPS_BY_SUBJECT -> {
                check(selectedSubjectsId >= 0)
                markScaleGroups = markDao.getUsedMarkScaleGroupsBySubject(selectedSubjectsId)
            }
            REQUEST_GET_TERMS -> {
                terms = db.termDao.getTermsShortInfo()
            }
            REQUEST_IMPORT_MARKS -> {
                var startTime = 0L
                var endTime = 0L
                AppDatabase.getAppDatabase(context)
                        .termDao.getStartEnd(selectedTermsId)?.also {
                    startTime = it.startDate
                    endTime = it.endDate
                }

                val marks = markDao.getMarksToImport(
                        selectedSubjectsId, selectedMarkScaleGroupsId,
                        startTime, endTime)

                val (converted, state) = convertMarksToImport2VirtualMarks(marks)

                markDao.clearVirtualMarks()
                MobiregPreferences.get(context).setSavedMarksState(state, selectedMarkScaleGroupsId)
                markDao.insertVirtualMarks(converted)
            }
        }

        currentRealizedRequest.postValue(currentRequestedState)
    }

    private fun convertMarksToImport2VirtualMarks(marksToImport: List<MarkDao.MarkToImport>)
            : Pair<List<VirtualMarkEntity>, Int>/*list and state*/ {
        if (marksToImport.isEmpty())
            return Pair(emptyList(), AboutVirtualMarksFragment.STATE_NO_MARKS_SAVED)

        if (marksToImport.any { it.markValue != null }) {
            // mamy oceny punktowe!
            return marksToImport.map {
                VirtualMarkEntity(0, TYPE_POINTS_SINGLE, it.markValue ?: 0f, it.weight)
            } to AboutVirtualMarksFragment.STATE_HAVING_POINTS_MARKS
        } else {
            // mamy oceny ze skali

            val outputList = ArrayList<VirtualMarkEntity>(marksToImport.size * 2)

            marksToImport.groupBy { it.parentId }.forEach { entry ->
                if (entry.value.size == 1) {
                    // pojedyncza ocena
                    val it = entry.value.first()
                    outputList.add(VirtualMarkEntity(0, TYPE_SCALE_SINGLE, it.scaleId!!.toFloat(), it.weight))
                } else {
                    val first = entry.value.first()
                    outputList.add(VirtualMarkEntity(0, TYPE_SCALE_PARENT, first.parentType!!.toFloat(), first.weight))

                    entry.value.asReversed().mapTo(outputList) {
                        VirtualMarkEntity(0, TYPE_SCALE_CHILD, it.scaleId!!.toFloat(), 0f)
                    }
                }
            }
            return outputList to AboutVirtualMarksFragment.STATE_HAVING_SCALE_MARKS
        }
    }
}
