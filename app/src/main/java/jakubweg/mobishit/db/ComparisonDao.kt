package jakubweg.mobishit.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface ComparisonDao {

    @Query("DELETE FROM ComparisonCaches")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertComparisons(values: List<ComparisonCacheData>)

    @Query("SELECT * FROM ComparisonCaches")
    fun getAll(): List<ComparisonCacheData>
}
