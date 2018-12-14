package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MessageDao

class MessagesListModel(application: Application)
    : BaseViewModel(application) {

    private val mReceivedMessages = MutableLiveData<List<MessageDao.MessageShortInfo>>()

    val receivedMessages get() = handleBackground(mReceivedMessages).asImmutable

    var sentMessages: LiveData<List<MessageDao.SentMessageShortData>> = MutableLiveData<List<MessageDao.SentMessageShortData>>()

    val sentMessagesLiveData = MutableLiveData<Long>()

    override fun doInBackground() {
        val dao = AppDatabase.getAppDatabase(context).messageDao

        sentMessages = dao.getAllSentMessages()

        mReceivedMessages.postValue(dao.getMessages())
        sentMessagesLiveData.postValue(System.currentTimeMillis())
    }
}