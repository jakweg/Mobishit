package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface TestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTests(tests: List<TestData>)

    @Query("DELETE FROM Tests")
    fun deleteAll()

    @Query("SELECT * FROM Tests ORDER BY date DESC")
    fun getAllTests(): List<TestData>
}