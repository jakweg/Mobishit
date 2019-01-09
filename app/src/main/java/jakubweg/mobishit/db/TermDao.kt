package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query

@Dao
interface TermDao {

    class TermShortInfo(val id: Int, val name: String, val type: String) {
        override fun toString() = name
    }

    @Query("SELECT id, name, type FROM Terms ORDER BY type DESC")
    fun getTermsShortInfo(): List<TermShortInfo>


    @Query("SELECT * from Terms ORDER BY type DESC")
    fun getTerms(): List<TermData>

    class StartEnd(val startDate: Long, val endDate: Long)

    @Query("SELECT startDate, endDate FROM Terms WHERE id = :id LIMIT 1")
    fun getStartEnd(id: Int): StartEnd?
}
