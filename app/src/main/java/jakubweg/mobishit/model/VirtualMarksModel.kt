package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.db.TermDao
import jakubweg.mobishit.fragment.VirtualMarksFragment
import jakubweg.mobishit.helper.*


enum class State {
    INITIALIZING,

    NOTHING,
    CHOOSE_SUBJECT,
    CHOOSE_SCALE_GROUP,
    CHOOSE_TERM,
    IMPORT_MARKS_BY_ARGUMENTS,
    IMPORTED,

    PREPARE_FOR_SCALE_MARKS,
    PREPARE_FOR_POINTS_MARKS,
}

class VirtualMarksModel(app: Application) :
        BaseViewModel(app) {

    var marksList: MutableList<VirtualMarkListItem> = mutableListOf()

    var subjectId = -1
    var markScaleGroupId = -1
    var termId = -1
    var startTime = Long.MIN_VALUE
    var endTime = Long.MAX_VALUE

    var subjects: List<MarkDao.SubjectShortInfo> = emptyList()
    var markScaleGroups: List<MarkDao.MarkScaleGroupShortInfo> = emptyList()
    var terms: List<TermDao.TermShortInfo> = emptyList()
    var markScales: List<MarkDao.MarkScaleShortInfo> = emptyList()
    var marksToImport: List<MarkDao.MarkToImport> = emptyList()


    val currentState: MutableLiveData<State> = MutableLiveData<State>().apply { State.INITIALIZING }
    private var requestedState = State.INITIALIZING

    fun requestState(state: State) {
        if (requestedState != state && currentState.value != state) {
            requestedState = state
            rerunTask()
        }
    }

    override fun onCleared() {
        SavingTask(AppDatabase.getAppDatabase(context).markDao,
                MobiregPreferences.get(context),
                markScaleGroupId,
                marksList).execute()
    }

    private class SavingTask(private val dao: MarkDao,
                             private val prefs: MobiregPreferences,
                             private val markScaleGroupId: Int,
                             private val marksList: MutableList<VirtualMarkListItem>)
        : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            dao.clearVirtualMarks()
            if (marksList.isEmpty()) {
                prefs.hasSavedAnyVirtualMark = false
            } else {
                prefs.apply {
                    lastMarkScaleGroupId = markScaleGroupId
                    hasSavedAnyVirtualMark = true
                }
                dao.insertVirtualMarks(marksList.map(VirtualMarkListItem::asEntity))
            }
        }
    }

    override fun doInBackground() {
        val markDao = AppDatabase.getAppDatabase(context).markDao
        when (requestedState) {
            State.NOTHING -> {
                // clear data
                subjectId = -1
                markScaleGroupId = -1
                termId = -1
                startTime = Long.MIN_VALUE
                endTime = Long.MAX_VALUE

                val prefs = MobiregPreferences.get(context)
                if (prefs.hasSavedAnyVirtualMark) {
                    var previousParent: VirtualMarkParent? = null
                    val restored = mutableListOf<VirtualMarkListItem>()
                    markDao.getVirtualMarksEntities().mapTo(restored) {
                        return@mapTo when (it.type) {
                            VirtualMarksFragment.VirtualBaseAdapter.TYPE_SCALE_PARENT ->
                                VirtualMarkParent().apply {
                                    parentType = it.value.toInt()
                                    weight = it.weight
                                    previousParent = this
                                }


                            VirtualMarksFragment.VirtualBaseAdapter.TYPE_SCALE_CHILD ->
                                VirtualMarkChild(previousParent).apply {
                                    markScaleIndex = it.value.toInt()
                                }

                            VirtualMarksFragment.VirtualBaseAdapter.TYPE_SCALE_SINGLE ->
                                VirtualMarkSingle().apply {
                                    markScaleIndex = it.value.toInt()
                                    weight = it.weight
                                }


                            VirtualMarksFragment.VirtualBaseAdapter.TYPE_POINTS_SINGLE ->
                                VirtualPointsMark().apply {
                                    gotPointsSum = it.value
                                    basePointsSum = it.weight
                                }

                            else -> throw IllegalArgumentException()
                        }
                    }

                    if (restored.isNotEmpty()) {
                        marksList = restored
                        if (restored.any {
                                    it.type != VirtualMarksFragment
                                            .VirtualBaseAdapter.TYPE_POINTS_SINGLE
                                }) {
                            // musimy jescze pobrać wartości skali dla ocen
                            // ale najpierw załadować z preferencji markScaleGroupId
                            markScaleGroupId = prefs.lastMarkScaleGroupId
                            markScales = markDao.getMarkScalesByGroupId(markScaleGroupId)
                        }
                        currentState.postValue(State.IMPORTED)
                        return
                    }
                }
            }
            State.CHOOSE_SUBJECT -> {
                subjects = markDao.getSubjectsWithCountedUsersMarks()
            }
            State.CHOOSE_SCALE_GROUP -> {
                markScaleGroups = if (subjectId <= 0)
                    markDao.getUsedMarkScaleGroups()
                else
                    markDao.getUsedMarkScaleGroupsBySubject(subjectId)
            }
            State.CHOOSE_TERM -> {
                terms = AppDatabase.getAppDatabase(context).termDao
                        .getTermsShortInfo()
            }
            State.IMPORT_MARKS_BY_ARGUMENTS -> {
                if (termId > 0) {
                    AppDatabase.getAppDatabase(context)
                            .termDao.getStartEnd(termId)?.also {
                        startTime = it.startDate
                        endTime = it.endDate
                    }
                }

                val marks =
                        markDao.getMarksToImport(subjectId, markScaleGroupId,
                                startTime, endTime)
                if (marks.none { it.markValue != null }) {
                    // musimy jescze pobrać wartości skali dla ocen
                    markScales = markDao.getMarkScalesByGroupId(markScaleGroupId)
                }
                marksToImport = marks
            }
            State.PREPARE_FOR_SCALE_MARKS -> {
                markScales = markDao.getMarkScalesByGroupId(markScaleGroupId)
            }
            else -> {
            }
        }
        currentState.postValue(requestedState)
    }
}
