import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_POINTS_SINGLE
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_CHILD
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_PARENT
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_SINGLE
import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.fragment.AboutVirtualMarksFragment
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.model.BaseViewModel

class VirtualMarksModel(app: Application) : BaseViewModel(app) {

    val marks = MutableLiveData<List<VirtualMarkBase>>()
    var markScales = listOf<MarkDao.MarkScaleShortInfo>()

    fun requestLoad() {
        handleBackground()
    }

    override fun doInBackground() {
        val markDao = AppDatabase.getAppDatabase(context).markDao

        val prefs = MobiregPreferences.get(context)
        val acceptOnlyPoints = prefs.savedVirtualMarksState == AboutVirtualMarksFragment.STATE_HAVING_POINTS_MARKS
        if (!acceptOnlyPoints)
            markScales = markDao.getMarkScalesByGroupId(prefs.savedMarkScaleGroupId)
        val entities = markDao.getVirtualMarksEntities()



        marks.postValue(entities.mapNotNull { it ->
            when (it.type) {
                TYPE_POINTS_SINGLE -> VirtualMarkPoints(it.value, it.weight)
                TYPE_SCALE_SINGLE -> {
                    val scaleId = it.value.toInt()
                    val index = markScales.indexOfFirst { it.id == scaleId }
                    if (index == -1)
                        return@mapNotNull null
                    VirtualMarkScaleSingle(index, it.weight)
                }
                TYPE_SCALE_PARENT -> VirtualMarkParent(it.value.toInt(), it.weight)

                TYPE_SCALE_CHILD -> {
                    val scaleId = it.value.toInt()
                    val index = markScales.indexOfFirst { it.id == scaleId }
                    if (index == -1)
                        return@mapNotNull null
                    VirtualMarkChild(index)
                }
                else -> throw IllegalStateException()
            }
        }.run {
            if (acceptOnlyPoints)
                filter { it.type == TYPE_POINTS_SINGLE }
            else
                filter { it.type != TYPE_POINTS_SINGLE }
        })
    }

    override fun onCleared() {
        SaveMarksTask(AppDatabase.getAppDatabase(context).markDao,
                marks.value ?: return,
                markScales).execute()
    }

    private class SaveMarksTask(
            private val markDao: MarkDao,
            private val marks: List<VirtualMarkBase>,
            private val markScales: List<MarkDao.MarkScaleShortInfo>)
        : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            markDao.clearVirtualMarks()
            markDao.insertVirtualMarks(marks.map { it.toDatabaseEntity(markScales) })
        }
    }
}
