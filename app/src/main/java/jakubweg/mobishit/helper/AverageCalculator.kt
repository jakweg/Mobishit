package jakubweg.mobishit.helper

import android.content.Context
import android.util.Log
import android.util.SparseArray
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.fragment.SubjectsMarkFragment
import java.util.ArrayList
import kotlin.Comparator

@Suppress("NOTHING_TO_INLINE")
class AverageCalculator private constructor() {

    companion object {
        private inline fun Float.zeroIfNan() = if (isNaN()) 0f else this

        fun getOrCreateAverageCacheData(context: Context): List<AverageCacheData> {
            val preferences = MobiregPreferences.get(context)
            val markDao = AppDatabase.getAppDatabase(context).markDao
            return if (preferences.hasReadyAverageCache)
                markDao.getAllAverageCache()
            else {
                val data = createAverageCacheData(context)
                markDao.clearAverageCache()
                data.forEach {
                    it.weightedAverage = it.weightedAverage.zeroIfNan()
                    it.gotPointsSum = it.gotPointsSum.zeroIfNan()
                    it.baseSum = it.baseSum.zeroIfNan()
                }
                markDao.insertAverageCache(data)
                preferences.hasReadyAverageCache = true
                data
            }
        }

        private fun createAverageCacheData(context: Context): List<AverageCacheData> {
            val db = AppDatabase.getAppDatabase(context)
            val markDao = db.markDao

            checkIfSelectedTermIsValid(context, db.termDao.getTermIds())

            val termId = MobiregPreferences.get(context).lastSelectedTerm
            val term = db.termDao.getStartEnd(termId)

            val subjects = markDao.getSubjectsWithUsersMarks(term.startDate, term.endDate)
            val output = ArrayList<AverageCacheData>(subjects.size)
            for (subject in subjects) {
                val marks = markDao.getMarksBySubject(subject.id)
                val result = calculateAverage(marks)

                output.add(AverageCacheData(0, subject.id, subject.name, null,
                        result.first, result.second, result.third).also { data ->
                    data.setMarksList(marks)
                })

            }

            return output
        }


        private const val ORDER_NEW_FIRST = 0
        private const val ORDER_OLD_FIRST = 1
        private const val ORDER_WORSE_FIRST = 2
        private const val ORDER_BETTER_FIRST = 3
        private const val ORDER_BY_NAME = 4
        private const val ORDER_BIG_WEIGHT_FIRST = 6

        const val ORDER_DEFAULT = ORDER_NEW_FIRST

        fun getOrderMethodsNames() = arrayOf(
                "Od najnowszych",
                "Od najstarszych",
                "Od najlepszych",
                "Od najgorszych",
                "Alfabetycznie",
                "Od bardziej znaczących"
        )

        fun getOrderMethodsIds() = intArrayOf(
                ORDER_NEW_FIRST,
                ORDER_OLD_FIRST,
                ORDER_BETTER_FIRST,
                ORDER_WORSE_FIRST,
                ORDER_BY_NAME,
                ORDER_BIG_WEIGHT_FIRST
        )


        private fun getComparator(orderBy: Int): Comparator<MarkDao.MarkShortInfo> {
            return when (orderBy) {
                ORDER_NEW_FIRST -> Comparator { o1, o2 -> o2.addTime.compareTo(o1.addTime) }
                ORDER_OLD_FIRST -> Comparator { o1, o2 -> o1.addTime.compareTo(o2.addTime) }
                ORDER_BY_NAME -> Comparator { o1, o2 -> o1.description.compareTo(o2.description) }
                ORDER_BETTER_FIRST -> Comparator { o1, o2 ->
                    if (o1.countPointsWithoutBase == true || o1.noCountToAverage == true)
                        return@Comparator 1

                    if (o2.countPointsWithoutBase == true || o2.noCountToAverage == true)
                        return@Comparator -1

                    return@Comparator (o2.markScaleValue + o2.markPointsValue / o2.markValueMax)
                            .compareTo(o1.markScaleValue + o1.markPointsValue / o1.markValueMax)
                }
                ORDER_WORSE_FIRST -> Comparator { o1, o2 ->
                    if (o1.countPointsWithoutBase == true || o1.noCountToAverage == true)
                        return@Comparator 1

                    if (o2.countPointsWithoutBase == true || o2.noCountToAverage == true)
                        return@Comparator -1

                    return@Comparator (o1.markScaleValue + o1.markPointsValue / o1.markValueMax)
                            .compareTo(o2.markScaleValue + o2.markPointsValue / o2.markValueMax)
                }
                ORDER_BIG_WEIGHT_FIRST -> Comparator<MarkDao.MarkShortInfo> { o1, o2 ->
                    if (o1.countPointsWithoutBase == true || o1.noCountToAverage == true)
                        return@Comparator 1

                    if (o2.countPointsWithoutBase == true || o2.noCountToAverage == true)
                        return@Comparator -1

                    return@Comparator (o2.weight + o2.markValueMax)
                            .compareTo(o1.weight + o1.markValueMax)
                }.then(getComparator(ORDER_NEW_FIRST))
                else -> throw IllegalArgumentException()
            }
        }

        private fun groupMarksByParents(marks: List<MarkDao.MarkShortInfo>, orderBy: Int)
                : List<MarkDao.MarkShortInfo> {
            val comparator = getComparator(orderBy)
            val grouped = marks
                    .asSequence()
                    .sortedWith(comparator)
                    .groupBy { it.parentIdOrSelf }
            val outputList = ArrayList<MarkDao.MarkShortInfo>(marks.size)

            grouped.values.forEach { it ->
                when {
                    it.size == 1 -> outputList.add(it.first().apply { viewType = SubjectsMarkFragment.MarkAdapter.TYPE_SINGLE })
                    it.size == 2 -> {
                        outputList.add(it.first().apply { viewType = SubjectsMarkFragment.MarkAdapter.TYPE_PARENT_FIRST })
                        outputList.add(it.last().apply { viewType = SubjectsMarkFragment.MarkAdapter.TYPE_PARENT_LAST })
                    }
                    else -> {
                        it.forEachIndexed { index, mark ->
                            mark.viewType = when (index) {
                                0 -> SubjectsMarkFragment.MarkAdapter.TYPE_PARENT_FIRST
                                it.size - 1 -> SubjectsMarkFragment.MarkAdapter.TYPE_PARENT_LAST
                                else -> SubjectsMarkFragment.MarkAdapter.TYPE_PARENT_MIDDLE
                            }
                        }
                        outputList.addAll(it)
                    }
                }
            }
            return outputList
        }

        private fun sortMarks(marks: List<MarkDao.MarkShortInfo>, order: Int, groupByParents: Boolean)
                : List<MarkDao.MarkShortInfo> {
            return if (groupByParents)
                groupMarksByParents(marks, order)
            else
                marks.sortedWith(getComparator(order))
        }

        fun getMarksAndCalculateAverage(context: Context, subjectId: Int, sortOrder: Int, groupByParents: Boolean):
                Pair<SparseArray<List<MarkDao.MarkShortInfo>>,
                        SparseArray<AverageCacheData>> {
            val db = AppDatabase.getAppDatabase(context)
            val dao = db.markDao
            val terms = db.termDao.getTermsShortInfo()

            checkIfSelectedTermIsValid(context, terms.map { it.id })

            val allMarks = sortMarks(dao.getMarksBySubject(subjectId), sortOrder, groupByParents)

            // termId, marks
            val marksMap = SparseArray<List<MarkDao.MarkShortInfo>>(terms.size)
            val averagesMap = SparseArray<AverageCacheData>(terms.size)

            terms.forEach { term ->
                val marks = when (term.type) {
                    "Y" -> allMarks
                    "T" -> mutableListOf<MarkDao.MarkShortInfo>().apply {
                        addAll(allMarks.filter { it.termId == term.id })
                    }
                    else -> throw IllegalArgumentException("unknown term type")
                }

                marksMap.put(term.id, marks)
                averagesMap.put(term.id, AverageCalculator.calculateAverage(marks).toCacheData())
            }
            return Pair(marksMap, averagesMap)
        }


        private fun checkIfSelectedTermIsValid(context: Context,
                                               terms: List<Int>) {
            MobiregPreferences.get(context).apply {
                val id = lastSelectedTerm
                if (terms.find { it == id } == null)
                    lastSelectedTerm = terms.firstOrNull() ?: 0
            }
        }

        private inline fun calculateAverage(marks: List<MarkDao.MarkShortInfo>)
                : Triple<Float, Float, Float> {
            return AverageCalculator().calculateAverage(marks)
        }


        private inline fun Triple<Float, Float, Float>.toCacheData(): AverageCacheData {
            return AverageCacheData(0, 0, null, null, first, second, third)
        }

    }

    private var averageSum = 0f
    private var averageWeight = 0f
    private var baseSum = 0f
    private var gotPointsSum = 0f
    private fun calculateAverage(marks: List<MarkDao.MarkShortInfo>): Triple<Float, Float, Float> {
        averageSum = 0f
        averageWeight = 0f
        baseSum = 0f
        gotPointsSum = 0f

        marks.markEveryNotUsed().forEach { mark ->
            if (mark.parentType == null)
                handleMarkWithoutParent(mark)
            else {
                val parentId = mark.parentIdOrSelf
                val matchingMarks = marks.filter {
                    !it.hasCalculatedAverage &&
                            (it.parentId == parentId ||
                                    it.markGroupId == parentId)
                }
                if (matchingMarks.isNotEmpty())
                    when (mark.parentType) {
                        MarkDao.PARENT_TYPE_COUNT_AVERAGE ->
                            handleParentCountAverage(matchingMarks)
                        MarkDao.PARENT_TYPE_COUNT_BEST ->
                            handleParentCountBest(matchingMarks)
                        MarkDao.PARENT_TYPE_COUNT_LAST ->
                            handleParentCountLast(matchingMarks)
                        MarkDao.PARENT_TYPE_COUNT_EVERY ->
                            handleParentCountEvery(matchingMarks)
                        MarkDao.PARENT_TYPE_COUNT_WORSE ->
                            handleParentCountWorse(matchingMarks)
                        else -> {
                            Log.i("AverageCalculator", "unknown parent type (${mark.parentType})")
                            //handleMarkWithoutParent(mark) //don't you can't calculate this, so don't even try
                        }
                    }
            }
        }


        return Triple(averageSum / averageWeight, gotPointsSum, baseSum)
    }

    private fun handleMarkWithoutParent(mark: MarkDao.MarkShortInfo, ignorePreviousCalculation: Boolean = false) {
        mark.apply {
            if (!ignorePreviousCalculation && hasCalculatedAverage) return
            hasCalculatedAverage = true
            if (markScaleValue >= 0 && noCountToAverage != null) {
                val weight = if (weight >= 0f) weight else 1f
                averageSum += markScaleValue * weight
                if (!noCountToAverage)
                    averageWeight += weight

            }
            if (countPointsWithoutBase != null) {
                if (markPointsValue >= 0)
                    gotPointsSum += markPointsValue
                if (!countPointsWithoutBase && markValueMax >= 0)
                    baseSum += markValueMax
            }
        }
    }

    private fun handleParentCountAverage(marks: List<MarkDao.MarkShortInfo>) {
        if (marks.isEmpty()) return
        var averageValue = 0f
        var marksCount = 0
        var gotPointsSum = 0f
        marks.forEach { mark ->
            mark.hasCalculatedAverage = true
            mark.apply {
                if (markScaleValue >= 0 && noCountToAverage != null) {
                    averageValue += markScaleValue
                    marksCount++

                } else if (countPointsWithoutBase != null &&
                        markPointsValue >= 0 &&
                        markValueMax >= 0) {
                    gotPointsSum += markPointsValue
                    marksCount++
                }
            }
        }

        val weight = (marks.firstOrNull { it.weight >= 0 }
                ?.weight ?: 1f)

        this.averageSum += averageValue * weight / marksCount.toFloat()
        this.averageWeight += weight

        this.gotPointsSum += gotPointsSum / marksCount.toFloat()
        this.baseSum += (marks.firstOrNull { it.markValueMax >= 0 }?.markValueMax ?: 0f)
    }

    private fun handleParentCountBest(marks: List<MarkDao.MarkShortInfo>) {
        handleMarkWithoutParent(
                marks
                        .markEveryUsed()
                        .maxBy {
                            when {
                                it.markScaleValue >= 0 -> it.markScaleValue
                                it.markPointsValue >= 0 -> it.markPointsValue
                                else -> Float.MIN_VALUE
                            }
                        } ?: return, true)
    }

    private fun handleParentCountWorse(marks: List<MarkDao.MarkShortInfo>) {
        handleMarkWithoutParent(
                marks
                        .markEveryUsed()
                        .minBy {
                            when {
                                it.markScaleValue >= 0 -> it.markScaleValue
                                it.markPointsValue >= 0 -> it.markPointsValue
                                else -> Float.MIN_VALUE
                            }
                        } ?: return, true)
    }

    private fun handleParentCountLast(marks: List<MarkDao.MarkShortInfo>) {
        handleMarkWithoutParent(marks
                .apply { forEach { it.hasCalculatedAverage = true } }
                .maxBy { it.addTime }
                ?: return, true)
    }

    private fun handleParentCountEvery(marks: List<MarkDao.MarkShortInfo>) {
        marks.forEach { handleMarkWithoutParent(it) }
    }


    private inline fun List<MarkDao.MarkShortInfo>.markEveryUsed()
            : List<MarkDao.MarkShortInfo> {
        forEach { it.hasCalculatedAverage = true }
        return this
    }

    private inline fun List<MarkDao.MarkShortInfo>.markEveryNotUsed()
            : List<MarkDao.MarkShortInfo> {
        forEach { it.hasCalculatedAverage = false }
        return this
    }
}
