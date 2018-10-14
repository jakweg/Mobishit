package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MessageDao
import jakubweg.mobishit.helper.DateHelper

class MessageDetailModel(application: Application)
    : BaseViewModel(application) {

    private var mMessageId = 0
    fun init(messageId: Int) {
        check(messageId != 0)
        if (mMessageId == messageId) return
        require(mMessageId == 0)
        mMessageId = messageId
    }

    private val mDetails = MutableLiveData<MessageDao.MessageLongInfo>()

    val details get() = handleBackground(mDetails).asImmutable

    override fun doInBackground() {
        check(mMessageId != 0) { "MessageDetailModel.init not called" }
        val dao = AppDatabase.getAppDatabase(context).messageDao

        mDetails.postValue(dao.getMessageInfo(mMessageId))

        dao.updateReadTime(mMessageId, DateHelper.getNowDateMillis())
    }
}