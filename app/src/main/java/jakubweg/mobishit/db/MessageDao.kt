package jakubweg.mobishit.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import jakubweg.mobishit.helper.DateHelper

@Dao
interface MessageDao {
    companion object {
        const val KIND_JUST_MESSAGE = -1
        const val KIND_NEUTRAL_REPRIMAND = 0
        const val KIND_POSITIVE_REPRIMAND = 1
        const val KIND_NEGATIVE_REPRIMAND = 2
    }

    class MessageShortInfo(val id: Int, val title: String?, val kind: Int, val sender: String, val sendTime: Long)

    @Query("""SELECT Messages.id, kind,  title, IFNULL(name || ' ' || surname, 'Od nieznanego (' || Messages.senderId || ')') AS sender, sendTime
            FROM Messages LEFT JOIN Teachers ON Teachers.id = Messages.senderId ORDER BY sendTime DESC""")
    fun getMessages(): List<MessageShortInfo>


    class MessageLongInfo(val title: String?, val kind: Int, val sender: String?, val senderId: Int, val sendTime: Long, val content: String) {
        @Ignore
        val formattedSendTime = DateHelper.millisToStringTime(sendTime)
    }

    @Query("""
        SELECT title, kind, IFNULL(name || ' ' || surname, 'Nieznany (' || senderId || ')') AS sender, senderId, sendTime, content FROM Messages
        LEFT JOIN Teachers ON Teachers.id = Messages.senderId WHERE Messages.id = :messageId LIMIT 1""")
    fun getMessageInfo(messageId: Int): MessageLongInfo


    @Query("SELECT name || ' ' || surname FROM Teachers WHERE id = :teacherId LIMIT 1")
    fun getTeacherFullName(teacherId: Int): String?


    @Query("UPDATE Messages SET readTime = :time WHERE id = :id")
    fun updateReadTime(id: Int, time: Long)

    class TeacherIdAndName(val id: Int, val fullName: String) {
        override fun toString() = fullName
    }

    @Query("SELECT id, name || ' ' || surname as fullName FROM Teachers ORDER BY name,surname")
    fun getTeachers(): List<TeacherIdAndName>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSentRequest(obj: SentMessageData): Long

    class SentMessageShortData(val fullName: String?, val subject: String, val sentTime: Long, val receiverId: Int, val status: Int) {
        fun statusAsString() = when (status) {
            SentMessageData.STATUS_ENQUEUED -> "Oczekuje na wysłanie"
            SentMessageData.STATUS_FAILED -> "Niepowodzenie wysłania"
            SentMessageData.STATUS_IN_PROGRESS -> "Wysyłanie…"
            SentMessageData.STATUS_SUCCEEDED -> "Wysłana"
            SentMessageData.STATUS_CANCELLED -> "Anulowana"
            else -> throw IllegalStateException("unknown status")
        }
    }

    @Query("""SELECT name || ' ' || surname as fullName, subject, sentTime, status, receiverId FROM SentMessages
        LEFT OUTER JOIN Teachers ON Teachers.id = receiverId
        ORDER BY sentTime DESC
    """)
    fun getAllSentMessages(): LiveData<List<SentMessageShortData>>

    class ShortInfoToSendMessage(val subject: String, val content: String, val receiverId: Int)

    @Query("SELECT subject, content, receiverId FROM SentMessages WHERE id = :id LIMIT 1")
    fun getMessageToSentById(id: Long): ShortInfoToSendMessage


    @Query("UPDATE SentMessages SET status = :status WHERE id = :id")
    fun markMessageStatus(id: Long, status: Int)
}
