package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import android.util.SparseBooleanArray
import jakubweg.mobishit.db.AppDatabase

class AboutAttendancesModel(application: Application) : BaseViewModel(application) {
    companion object {
        const val ACTION_SUBJECT_EXCLUDED_CHANGED = "subject_excluded"
    }
    open class TypeInfoAboutItemParent

    class TypeInfoAboutItem(val name: String) : TypeInfoAboutItemParent()


    fun updateExcludingSubjects(subjects: SparseBooleanArray) {
        UpdatingExcludedSubjectsTask(subjects, getApplication()).execute()
    }

    private class UpdatingExcludedSubjectsTask(
            private val subjects: SparseBooleanArray,
            private val application: Application
    ) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            val markDao = AppDatabase.getAppDatabase(application).markDao
            for (i in 0 until subjects.size()) {
                markDao.updateSubjectExcluding(subjects.keyAt(i), subjects.valueAt(i))
            }
            LocalBroadcastManager.getInstance(application)
                    .sendBroadcast(Intent().apply { action = ACTION_SUBJECT_EXCLUDED_CHANGED })
        }
    }


    private val mTypes = MutableLiveData<MutableList<TypeInfoAboutItemParent>>()

    val types
        get() = handleBackground(mTypes).asImmutable

    override fun doInBackground() {
        val types = AppDatabase
                .getAppDatabase(context)
                .attendanceDao
                .getTypesInfo()


        val outputTypes = mutableListOf<TypeInfoAboutItemParent>()
        var lastType: String? = null
        types.forEach {
            if (it.countAs != lastType) {
                when (it.countAs) {
                    "P" -> outputTypes.add(TypeInfoAboutItem("obecności:"))
                    "A" -> outputTypes.add(TypeInfoAboutItem("nieobecności:"))
                    "L" -> outputTypes.add(TypeInfoAboutItem("spóźnienia:"))
                    else -> outputTypes.add(TypeInfoAboutItem("inne:"))
                }
                lastType = it.countAs
            }
            outputTypes.add(it)
        }

        mTypes.postValue(outputTypes)
    }
}
