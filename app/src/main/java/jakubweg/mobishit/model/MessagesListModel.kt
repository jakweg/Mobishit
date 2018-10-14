package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MessageDao

class MessagesListModel(application: Application)
    : BaseViewModel(application) {

    private val mMessages = MutableLiveData<List<MessageDao.MessageShortInfo>>()

    val messages get() = handleBackground(mMessages).asImmutable

    override fun doInBackground() {
        val dao = AppDatabase.getAppDatabase(context).messageDao
        mMessages.postValue(dao.getMessages())
    }
}