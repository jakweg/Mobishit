package jakubweg.mobishit.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager

@Database(entities = [Teacher::class, RoomData::class, TermData::class, SubjectData::class, GroupData::class, GroupTerm::class, MarkScaleGroup::class, MarkScale::class, MarkDivisionGroup::class, MarkKind::class, MarkGroupGroup::class, MarkGroup::class, EventType::class, EventTypeTeacher::class, EventTypeTerm::class, EventTypeGroup::class, EventData::class, EventIssue::class, EventEvent::class, AttendanceType::class, AttendanceData::class, MarkData::class, StudentGroup::class, EventTypeSchedule::class, LessonData::class, MessageData::class, TestData::class, AverageCacheData::class],
        version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val mainDao: MainDao

    abstract val markDao: MarkDao

    abstract val messageDao: MessageDao

    abstract val eventDao: EventDao

    abstract val testDao: TestDao

    companion object {
        private const val ACTION_UPDATED = "dbUpdated"
        const val EXTRA_HAS_CHANGED = "changed"

        val databaseUpdatedIntentFilter
            get() = IntentFilter(ACTION_UPDATED)

        fun notifyUpdated(context: Context, databaseChanged: Boolean) {
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(Intent(ACTION_UPDATED).also {
                        it.putExtra(EXTRA_HAS_CHANGED, databaseChanged)
                    })
        }


        private var INSTANCE: AppDatabase? = null

        fun getAppDatabase(context: Context): AppDatabase {
            return INSTANCE ?: Room.databaseBuilder(context.applicationContext,
                    AppDatabase::class.java, "mobireg.db")
                    .allowMainThreadQueries() // widget on homescreen uses main thread, so we can't remove it
                    .build()
                    .also { INSTANCE = it }
        }


        fun deleteDatabase(context: Context) {
            INSTANCE?.also {
                it.clearAllTables()
                it.close()
                INSTANCE = null
            }
            context.deleteDatabase("mobireg.db")
        }
    }
}