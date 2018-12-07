package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase

class AboutAttendancesModel(application: Application) : BaseViewModel(application) {
    open class TypeInfoAboutItemParent

    class TypeInfoAboutItem(val name: String) : TypeInfoAboutItemParent()

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
                    "A" -> outputTypes.add(TypeInfoAboutItem("nieobecności"))
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
