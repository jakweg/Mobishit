import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.os.AsyncTask
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.fragment.AboutVirtualMarksFragment
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_POINTS_SINGLE
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_SCALE_CHILD
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_SCALE_PARENT
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_SCALE_SINGLE
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

        val entities = when {
            prefs.shouldClearVirtualMarks -> {
                markDao.clearVirtualMarks()
                emptyList()
            }

            prefs.savedVirtualMarksState != AboutVirtualMarksFragment.STATE_NO_MARKS_SAVED -> {
                markDao.getVirtualMarksEntities()
            }
            else -> emptyList()
        }

        if (!acceptOnlyPoints)
            markScales = markDao.getMarkScalesByGroupId(prefs.savedMarkScaleGroupId)

        marks.postValue(entities.mapNotNull { entity ->
            when (entity.type) {
                TYPE_POINTS_SINGLE -> VirtualMarkPoints(entity.originalMarkId, entity.value, entity.weight)
                TYPE_SCALE_SINGLE -> {
                    val scaleId = entity.value.toInt()
                    val index = markScales.indexOfFirst { it.id == scaleId }
                    if (index == -1)
                        return@mapNotNull null
                    VirtualMarkScaleSingle(entity.originalMarkId, index, entity.weight)
                }
                TYPE_SCALE_PARENT -> VirtualMarkParent(entity.value.toInt(), entity.weight,  entity.weight)

                TYPE_SCALE_CHILD -> {
                    val scaleId = entity.value.toInt()
                    val index = markScales.indexOfFirst { it.id == scaleId }
                    if (index == -1)
                        return@mapNotNull null
                    VirtualMarkChild(entity.originalMarkId, index)
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
