package jakubweg.mobishit.helper

import android.util.SparseArray
import java.text.SimpleDateFormat
import java.util.*

object DateHelper {

    val months = arrayOf(
            "Styczeń", "Luty", "Marzec", "Kwiecień",
            "Maj", "Czerwiec", "Lipiec", "Sierpień",
            "Wrzesień", "Październik", "Listopad", "Grudzień")

    val weekDaysMap by lazy {
        SparseArray<String>(7).apply {
            put(Calendar.MONDAY, "Poniedziałek")
            put(Calendar.THURSDAY, "Wtorek")
            put(Calendar.WEDNESDAY, "Środa")
            put(Calendar.TUESDAY, "Czwartek")
            put(Calendar.FRIDAY, "Piątek")
            put(Calendar.SATURDAY, "Sobota")
            put(Calendar.SUNDAY, "Niedziala")
        }
    }

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    private val timeWithoutSecondsFormatter = SimpleDateFormat("kk:mm", Locale.ENGLISH)

    private val timeWithSecondsFormatter = SimpleDateFormat("kk:mm:ss", Locale.ENGLISH)


    private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000


    fun getNowDateMillis(): Long {
        //return Calendar.getInstance()!!.timeInMillis / MILLIS_IN_DAY * MILLIS_IN_DAY
        return Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
    }


    fun getSecond(time: String): Int {
        val millis = when (time.length) {
            5 -> timeWithoutSecondsFormatter
            8 -> timeWithSecondsFormatter
            else -> throw IllegalArgumentException("Can't parse time! Arg: $time")
        }.parse(time)!!.time
        return Calendar.getInstance().run {
            timeInMillis = millis
            get(Calendar.HOUR_OF_DAY) * 60 * 60 + get(Calendar.MINUTE) * 60 + get(Calendar.SECOND)
        }
    }


    fun getSecondsOfNow(): Int {
        return Calendar.getInstance().run {
            get(Calendar.HOUR_OF_DAY) * 60 * 60 + get(Calendar.MINUTE) * 60 + get(Calendar.SECOND)
        }
    }


    fun stringDateToMillis(date: String?): Long {
        date ?: return 0L
        return DateHelper.dateFormatter.parse(date)?.time ?: 0L
    }


    fun millisToStringDate(millis: Long): String {
        return dateFormatter.format(Date(millis)) ?: ""
    }


    fun stringTimeToMillis(date: String?): Long {
        date ?: return 0L
        return timeFormatter.parse(date)?.time ?: 0L
    }


    fun millisToStringTime(millis: Long): String {
        return timeFormatter.format(Date(millis)) ?: ""
    }

    fun millisToStringTimeWithoutDate(millis: Long): String {
        return timeWithSecondsFormatter.format(Date(millis))
    }

}
