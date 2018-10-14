package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Query
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

    @Query("""SELECT Messages.id, kind,  title, IFNULL(name || ' ' || surname, 'Od nieznanego') AS sender, sendTime
            FROM Messages LEFT JOIN Teachers ON Teachers.id = Messages.senderId ORDER BY sendTime DESC""")
    fun getMessages(): List<MessageShortInfo>


    class MessageLongInfo(val title: String?, val kind: Int, val sender: String?, val sendTime: Long, val content: String) {
        @Ignore
        val formattedSendTime = DateHelper.millisToStringTime(sendTime)
    }

    @Query("""
        SELECT title, kind, IFNULL(name || ' ' || surname, 'Nieznany') AS sender, sendTime, content FROM Messages
        LEFT JOIN Teachers ON Teachers.id = Messages.senderId WHERE Messages.id = :messageId LIMIT 1""")
    fun getMessageInfo(messageId: Int): MessageLongInfo


    @Query("SELECT name || ' ' || surname FROM Teachers WHERE id = :teacherId LIMIT 1")
    fun getTeacherFullName(teacherId: Int): String?


    @Query("UPDATE Messages SET readTime = :time WHERE id = :id")
    fun updateReadTime(id: Int, time: Long)
}
