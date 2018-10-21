package jakubweg.mobishit.helper

import android.content.Context
import android.support.v4.util.ArrayMap
import android.util.Log
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao

class AverageCalculator private constructor() {

    companion object {
        fun getMarksAndCalculateAverage(context: Context, subjectId: Int):
                Triple<List<MarkDao.TermShortInfo>,
                        ArrayMap<Int, List<MarkDao.MarkShortInfo>>,
                        ArrayMap<Int, AverageCalculationResult>> {
            val dao = AppDatabase.getAppDatabase(context).markDao
            val terms = dao.getTerms()

            val allMarks = dao.getMarksBySubject(subjectId)

            // termIs, marks
            val marksMap = ArrayMap<Int, List<MarkDao.MarkShortInfo>>(terms.size)
            val averagesMap = ArrayMap<Int, AverageCalculationResult>(terms.size)

            terms.forEach { term ->
                val marks = when (term.type) {
                    "Y" -> allMarks
                    "T" -> mutableListOf<MarkDao.MarkShortInfo>().apply {
                        addAll(allMarks.filter { it.termId == term.id })
                    }
                    else -> throw IllegalArgumentException("unknown term type")
                }

                marksMap[term.id] = marks
                averagesMap[term.id] = AverageCalculator.calculateAverage(marks)
            }
            return Triple(terms, marksMap, averagesMap)
        }

        fun calculateAverage(context: Context, termId: Int, subjectId: Int):
                AverageCalculationResult {
            val dao = AppDatabase.getAppDatabase(context).markDao
            val marks = dao.getMarksBySubjectAndTerm(termId, subjectId)
            return calculateAverage(marks)
        }

        private fun calculateAverage(marks: List<MarkDao.MarkAverageShortInfo>?): AverageCalculator.AverageCalculationResult {
            if (marks == null) throw NullPointerException()
            return AverageCalculator().calculateAverage(marks)
        }
    }

    class AverageCalculationResult(val weightedAverage: Float, val gotPointsSum: Float, val baseSum: Float) {
        val averageText by lazy(LazyThreadSafetyMode.NONE) {
            if (gotPointsSum != 0f || baseSum != 0f)
                "Zdobyte punkty: $gotPointsSum na $baseSum czyli ${(gotPointsSum / baseSum * 100f).toInt()}%"
            else if (weightedAverage > 0f)
                "Twoja średnia ważona wynosi ${String.format("%.2f", weightedAverage)}"
            else
                "Brak danych"
        }

        val shortAverageText by lazy(LazyThreadSafetyMode.NONE) {
            if (gotPointsSum != 0f || baseSum != 0f)
                "$gotPointsSum/$baseSum\n${(gotPointsSum / baseSum * 100f).toInt()}%"
            else if (weightedAverage > 0f)
                String.format("%.2f", weightedAverage)
            else
                ""
        }
    }

    private var averageSum = 0f
    private var averageWeight = 0f
    private var baseSum = 0f
    private var gotPointsSum = 0f
    private fun calculateAverage(marks: List<MarkDao.MarkAverageShortInfo>): AverageCalculator.AverageCalculationResult {
        averageSum = 0f
        averageWeight = 0f
        baseSum = 0f
        gotPointsSum = 0f

        marks.forEach { it.hasCalculatedAverage = false }
        marks.forEach { mark ->
            if (mark.parentType == null)
                handleMarkWithoutParent(mark)
            else {
                val parentId = mark.parentId ?: mark.markGroupId
                val matchingMarks = marks.filter {
                    !it.hasCalculatedAverage &&
                            (it.parentId == parentId ||
                                    it.markGroupId == parentId)
                }
                when (mark.parentType) {
                    MarkDao.PARENT_TYPE_COUNT_AVERAGE ->
                        handleParentCountAverage(matchingMarks)
                    MarkDao.PARENT_TYPE_COUNT_BEST ->
                        handleParentCountBest(matchingMarks)
                    MarkDao.PARENT_TYPE_COUNT_LAST ->
                        handleParentCountLast(matchingMarks)

                    else -> {
                        Log.i("AverageCalculator", "unknown parent type (${mark.parentType})")
                        handleMarkWithoutParent(mark)
                    }
                }
            }
        }


        return AverageCalculator.AverageCalculationResult(
                averageSum / averageWeight, gotPointsSum, baseSum)
    }

    private fun handleMarkWithoutParent(mark: MarkDao.MarkAverageShortInfo, ignorePreviousCalculation: Boolean = false) {
        mark.apply {
            if (!ignorePreviousCalculation && hasCalculatedAverage) return
            hasCalculatedAverage = true
            if (markScaleValue != null && noCountToAverage != null) {
                val weight = (defaultWeight ?: 1f)
                averageSum += markScaleValue * weight
                if (!noCountToAverage)
                    averageWeight += weight

            } else if (countPointsWithoutBase != null &&
                    markPointsValue != null &&
                    markValueMax != null) {
                gotPointsSum += markPointsValue
                if (!countPointsWithoutBase)
                    baseSum += markValueMax
            }
        }
    }

    private fun handleParentCountAverage(marks: List<MarkDao.MarkAverageShortInfo>) {
        if (marks.isEmpty()) return
        var averageValue = 0f
        var weightSum = 0f
        var baseSum = 0f
        var gotPointsSum = 0f
        var pointsMarksSum = 0
        marks.forEach { mark ->
            mark.hasCalculatedAverage = true
            mark.apply {
                if (markScaleValue != null && noCountToAverage != null) {
                    val weight = (defaultWeight ?: 1f)
                    averageValue += markScaleValue * weight
                    weightSum += weight

                } else if (countPointsWithoutBase != null &&
                        markPointsValue != null &&
                        markValueMax != null) {
                    gotPointsSum += markPointsValue
                    baseSum += markValueMax
                    pointsMarksSum++
                }
            }
        }

        this.averageSum += averageValue
        this.averageWeight += weightSum
        if (pointsMarksSum != 0) {
            this.baseSum += baseSum / pointsMarksSum
            this.gotPointsSum += gotPointsSum / pointsMarksSum
        }
    }

    private fun handleParentCountBest(marks: List<MarkDao.MarkAverageShortInfo>) {
        handleMarkWithoutParent(
                marks
                        .apply { forEach { it.hasCalculatedAverage = true } }
                        .maxBy {
                            when {
                                it.markScaleValue != null -> it.markScaleValue
                                it.markPointsValue != null -> it.markPointsValue
                                else -> Float.MIN_VALUE
                            }
                        } ?: return, true)
    }

    private fun handleParentCountLast(marks: List<MarkDao.MarkAverageShortInfo>) {
        handleMarkWithoutParent(marks
                .apply { forEach { it.hasCalculatedAverage = true } }
                .maxBy { it.addTime }
                ?: return, true)
    }
}