package jakubweg.mobishit.helper

import android.util.SparseArray
import java.text.SimpleDateFormat
import java.util.*

object DateHelper {

    val months by lazy(LazyThreadSafetyMode.NONE) {
        SparseArray<String>(12).apply {
            put(Calendar.JANUARY, "Styczeń")
            put(Calendar.FEBRUARY, "Luty")
            put(Calendar.MARCH, "Marzec")
            put(Calendar.APRIL, "Kwiecień")
            put(Calendar.MAY, "Maj")
            put(Calendar.JUNE, "Czerwiec")
            put(Calendar.JULY, "Lipiec")
            put(Calendar.AUGUST, "Sierpień")
            put(Calendar.SEPTEMBER, "Wrzesień")
            put(Calendar.OCTOBER, "Październik")
            put(Calendar.NOVEMBER, "Listopad")
            put(Calendar.DECEMBER, "Grudzień")
        }
    }

    private val monthsAbbreviations by lazy(LazyThreadSafetyMode.NONE) {
        SparseArray<String>(12).apply {
            put(Calendar.JANUARY, "sty")
            put(Calendar.FEBRUARY, "lut")
            put(Calendar.MARCH, "mar")
            put(Calendar.APRIL, "kwi")
            put(Calendar.MAY, "maj")
            put(Calendar.JUNE, "cze")
            put(Calendar.JULY, "lip")
            put(Calendar.AUGUST, "sie")
            put(Calendar.SEPTEMBER, "wrz")
            put(Calendar.OCTOBER, "paź")
            put(Calendar.NOVEMBER, "lis")
            put(Calendar.DECEMBER, "gru")
        }
    }

    val weekDaysMap by lazy(LazyThreadSafetyMode.NONE) {
        SparseArray<String>(7).apply {
            put(Calendar.MONDAY, "Poniedziałek")
            put(Calendar.TUESDAY, "Wtorek")
            put(Calendar.WEDNESDAY, "Środa")
            put(Calendar.THURSDAY, "Czwartek")
            put(Calendar.FRIDAY, "Piątek")
            put(Calendar.SATURDAY, "Sobota")
            put(Calendar.SUNDAY, "Niedziala")
        }
    }

    private val weekDaysAbbreviations by lazy(LazyThreadSafetyMode.NONE) {
        SparseArray<String>(7).apply {
            put(Calendar.MONDAY, "Pon")
            put(Calendar.TUESDAY, "Wt")
            put(Calendar.WEDNESDAY, "Śr")
            put(Calendar.THURSDAY, "Czw")
            put(Calendar.FRIDAY, "Pt")
            put(Calendar.SATURDAY, "Sob")
            put(Calendar.SUNDAY, "Niedz")
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


    fun formatPrettyDate(millis: Long) = buildString {
        val cal = Calendar.getInstance()!!
        cal.timeInMillis = millis

        append(weekDaysAbbreviations[cal.get(Calendar.DAY_OF_WEEK)])
        append("., ")
        append(cal.get(Calendar.DAY_OF_MONTH))
        append(' ')
        append(monthsAbbreviations[cal.get(Calendar.MONTH)])
    }
}
