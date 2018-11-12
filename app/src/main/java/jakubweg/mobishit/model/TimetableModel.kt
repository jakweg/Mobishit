package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.helper.DateHelper
import java.util.*

class TimetableModel(application: Application)
    : BaseViewModel(application) {

    class Date internal constructor(val time: Long, val formatted: String)

    var currentSelectedDayIndex = 0

    private val mDays = MutableLiveData<Array<Date>>()

    val days get() = handleBackground(mDays).asImmutable

    override fun doInBackground() {
        val days = doAtLeast(0L) {
            //350
            val dao = AppDatabase.getAppDatabase(context).eventDao

            val calc = Calendar.getInstance()

            val millis = if (calc.get(Calendar.HOUR_OF_DAY) > 16) (calc.timeInMillis + 24 * 60 * 60 * 1000L)
            else calc.timeInMillis

            val days = dao.getDaysWithEventsAsMillis()

            val index = days.indexOfFirst { it > millis } - 1
            currentSelectedDayIndex = if (index < 0) 0 else index

            val cal = Calendar.getInstance()
            val iterator = days.iterator()
            return@doAtLeast Array(days.size) { _ ->
                cal.timeInMillis = iterator.next()
                return@Array TimetableModel.Date(cal.timeInMillis,
                        "${DateHelper.millisToStringDate(cal.timeInMillis)}\n${getWeekdayName(cal.get(Calendar.DAY_OF_WEEK))}")
            }
        }
        mDays.postValue(days)
    }

    private fun getWeekdayName(day: Int): String {
        return when (day) {
            Calendar.MONDAY -> "PONIEDZIAŁEK"
            Calendar.TUESDAY -> "WTOREK"
            Calendar.WEDNESDAY -> "ŚRODA"
            Calendar.THURSDAY -> "CZWARTEK"
            Calendar.FRIDAY -> "PIĄTEK"
            Calendar.SATURDAY -> "SOBOTA"
            Calendar.SUNDAY -> "NIEDZIELA"
            else -> throw IllegalArgumentException()
        }
    }
}
