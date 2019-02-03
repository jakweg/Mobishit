package jakubweg.mobishit.model

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.helper.DateHelper
import java.util.*
import kotlin.collections.ArrayList

class TimetableModel(application: Application)
    : BaseViewModel(application) {

    class Date internal constructor(val time: Long, var formatted: String, val isActionButton: Boolean)

    var currentSelectedDayIndex = -1

    private var obtainedFirstAndLastDay = false
    private var firstDayOfSchool = 0L
    private var lastDayOfSchool = 0L

    private var requestedDate = -1L
    private var neighborhoodObjectLimit = 1

    fun requestDate(date: Long) {
        if (date == requestedDate)
            return
        neighborhoodObjectLimit += 3
        requestedDate = date
        cancelLastTask()
        handleBackground()
    }

    private val mDays = MutableLiveData<ArrayList<Date>>()
    val days get() = (mDays).asImmutable

    override fun doInBackground() {
        val dao = AppDatabase.getAppDatabase(context).eventDao

        if (!obtainedFirstAndLastDay) {
            obtainedFirstAndLastDay = true
            val firstDayOfSchool = dao.getFirstDayOfSchool()
            val lastDayOfSchool = dao.getLastDayOfSchool()
            if (firstDayOfSchool == null || lastDayOfSchool == null) {
                mDays.postValue(arrayListOf())
                return
            } else {
                this.firstDayOfSchool = firstDayOfSchool
                this.lastDayOfSchool = lastDayOfSchool
            }
        }

        var finalDays = mDays.value
        if (finalDays.isNullOrEmpty()) {
            val daysBefore = dao.getDaysWithEventsAsMillisTo(requestedDate, neighborhoodObjectLimit).reversedArray()
            val daysAfter = dao.getDaysWithEventsAsMillisSince(requestedDate, neighborhoodObjectLimit)
            finalDays = ArrayList(daysAfter.size + daysBefore.size + 2)

            if (daysBefore.isNotEmpty()) {
                var startIndex = 0
                if (firstDayOfSchool < daysBefore.first()) {
                    // są jeszcze dni przed
                    startIndex = 1
                    finalDays.add(TimetableModel.Date(daysBefore.first(), "Pokaż\npoprzednie dni", true))
                }
                for (i in startIndex until daysBefore.size)
                    finalDays.add(TimetableModel.Date(daysBefore[i], formatDate(daysBefore[i]), false))
            }

            if (daysAfter.isNotEmpty()) {
                var iterationCount = daysAfter.size
                if (lastDayOfSchool > daysAfter.last()) {
                    // są jescze dni po
                    iterationCount--
                }
                //for (i in (iterationCount - 1) downTo 0)
                for (i in 0 until iterationCount)
                    finalDays.add(TimetableModel.Date(daysAfter[i], formatDate(daysAfter[i]), false))
                if (iterationCount != daysAfter.size)
                    finalDays.add(TimetableModel.Date(daysAfter.last(), "Pokaż\nnastępne dni", true))
            }

            currentSelectedDayIndex = finalDays.indexOfLast { it.time <= requestedDate }

            //currentSelectedDayIndex = daysBefore.size
            //currentSelectedDayIndex = Math.max(daysBefore.size - 1, 0)
        } else {
            val currentFirst = finalDays.first()
            val currentLast = finalDays.last()

            if (requestedDate < currentFirst.time + 1L) {
                currentFirst.formatted = formatDate(currentFirst.time)

                val daysBefore = dao.getDaysWithEventsAsMillisTo(requestedDate, neighborhoodObjectLimit).reversedArray()
                if (daysBefore.isNotEmpty()) {
                    var startIndex = 0
                    val tmpArray = ArrayList<Date>(daysBefore.size + 1)
                    if (firstDayOfSchool < daysBefore.first()) {
                        // są jeszcze dni przed
                        startIndex = 1
                        tmpArray.add(TimetableModel.Date(daysBefore.first(), "Pokaż\npoprzednie dni", true))
                    }
                    for (i in startIndex until daysBefore.size)
                        tmpArray.add(TimetableModel.Date(daysBefore[i], formatDate(daysBefore[i]), false))

                    finalDays.addAll(0, tmpArray)
                    currentSelectedDayIndex += tmpArray.size


                    // fill with days between requestedDay and first already having day
                    val days = dao.getDaysWithEventAsMillisBetween(requestedDate, currentFirst.time)
                    if (days.isNotEmpty()) {
                        finalDays.ensureCapacity(finalDays.size + days.size)
                        finalDays.addAll(tmpArray.size, List(days.size) {
                            TimetableModel.Date(days[it], formatDate(days[it]), false)
                        })
                    }
                }


            } else if (requestedDate > currentLast.time - 1L) {

                val daysAfter = dao.getDaysWithEventsAsMillisSince(requestedDate, neighborhoodObjectLimit)
                if (daysAfter.isNotEmpty()) {
                    var iterationCount = daysAfter.size
                    if (lastDayOfSchool > daysAfter.last()) {
                        // są jescze dni po
                        iterationCount--
                    }

                    finalDays.removeAt(finalDays.size - 1)

                    val daysBetween = dao.getDaysWithEventAsMillisBetween(currentLast.time, requestedDate)
                    finalDays.ensureCapacity(finalDays.size + daysBetween.size + daysAfter.size + 1)
                    daysBetween.forEach { finalDays.add(TimetableModel.Date(it, formatDate(it), false)) }

                    for (i in 0 until iterationCount)
                        finalDays.add(TimetableModel.Date(daysAfter[i], formatDate(daysAfter[i]), false))
                    if (iterationCount != daysAfter.size)
                        finalDays.add(TimetableModel.Date(daysAfter.last(), "Pokaż\nnastępne dni", true))
                }
            }
        }

        mDays.postValue(finalDays)
    }

    private fun formatDate(date: Long): String {
        val cal = Calendar.getInstance()!!
        cal.timeInMillis = date
        return "${DateHelper.millisToStringDate(cal.timeInMillis)}\n" +
                getWeekdayName(cal.get(Calendar.DAY_OF_WEEK))
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
