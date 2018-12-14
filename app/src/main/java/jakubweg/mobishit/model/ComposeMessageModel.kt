package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MessageDao

class ComposeMessageModel(app: Application) : BaseViewModel(app) {

    private val mTeachers = MutableLiveData<List<MessageDao.TeacherIdAndName>>()
    val teachers get() = handleBackground(mTeachers).asImmutable
    var selectedTeacher: MessageDao.TeacherIdAndName? = null
    var selectedTeacherId: Int = 0

    override fun doInBackground() {
        mTeachers.postValue(
                AppDatabase.getAppDatabase(context)
                        .messageDao.getTeachers()
                        .filter { it.fullName.isNotBlank() }
        )
    }
}
