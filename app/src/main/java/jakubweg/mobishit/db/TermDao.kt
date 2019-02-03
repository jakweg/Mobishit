package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query

@Dao
interface TermDao {

    companion object {
        fun getNiceTermName(type: String?, name: String?): String {
            type!!
            name!!
            return when (type) {
                "Y" -> if (name.contains("rok", true)) name else "Rok szkolny $name"
                "T" -> if (name.contains("semestr", true)) name else "Semestr $name"
                else -> "Nieznany czas ($name)"
            }
        }
    }

    class TermShortInfo(val id: Int, val name: String, val type: String) {
        override fun toString() = name
    }

    @Query("SELECT id, name, type FROM Terms ORDER BY type DESC")
    fun getTermsShortInfo(): List<TermShortInfo>

    @Query("SELECT id, name, type FROM Terms WHERE id = :termId LIMIT 1")
    fun getTermShortInfo(termId: Int): TermShortInfo?


    @Query("SELECT * from Terms ORDER BY type DESC")
    fun getTerms(): List<TermData>

    class StartEnd(val startDate: Long, val endDate: Long)

    @Query("SELECT startDate, endDate FROM Terms WHERE id = :id LIMIT 1")
    fun getStartEnd(id: Int): StartEnd?
}
