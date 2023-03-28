package jakubweg.mobishit.helper

import android.content.res.Resources
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.text.HtmlCompat
import android.text.Spanned
import jakubweg.mobishit.R
import jakubweg.mobishit.db.MarkDao

object CommonFormatter {
    fun formatMarksAverage(gotPointsSum: Float, basePointsSum: Float, weightedAverage: Float, appResources: Resources, appTheme: Resources.Theme?, shortForm: Boolean) : Spanned {
        val hasPoints = gotPointsSum > 0f || basePointsSum > 0f
        val hasWeightedAverage = weightedAverage > 0f
        val isCriticalAverage = (hasWeightedAverage && weightedAverage < 2.0f) || (hasPoints && gotPointsSum / basePointsSum < 0.4f)
        val averagePrefix = if(isCriticalAverage) "<font color=${ResourcesCompat.getColor(appResources, R.color.criticalAverageTextColor, appTheme)}>" else ""
        val averagePostfix = if(isCriticalAverage)"</font>" else ""
        return HtmlCompat.fromHtml(
            if(shortForm) when {
                hasPoints && hasWeightedAverage ->
                    String.format("$averagePrefix%.2f$averagePostfix\n%.1f/%.1f $averagePrefix%.1f%%$averagePostfix",
                            weightedAverage, gotPointsSum, basePointsSum, gotPointsSum / basePointsSum * 100f)

                hasPoints ->
                    String.format("%.1f/%.1f\n$averagePrefix%.1f%%$averagePostfix",
                            gotPointsSum, basePointsSum, gotPointsSum / basePointsSum * 100f)

                hasWeightedAverage -> String.format("$averagePrefix%.2f$averagePostfix", weightedAverage)

                else -> ""
            } else when {
                hasPoints && hasWeightedAverage ->
                    String.format("Średnia: $averagePrefix%.2f$averagePostfix\nZdobyte punkty: %.1f na %.1f czyli $averagePrefix%.1f%%$averagePostfix",
                            weightedAverage, gotPointsSum, basePointsSum, gotPointsSum / basePointsSum * 100f)

                hasPoints ->
                    String.format("Zdobyte punkty: %.1f na %.1f czyli $averagePrefix%.1f%%$averagePostfix",
                            gotPointsSum, basePointsSum, gotPointsSum / basePointsSum * 100f)

                hasWeightedAverage ->
                    String.format("Twoja średnia ważona wynosi $averagePrefix%.2f$averagePostfix", weightedAverage)

                else -> "Brak danych" },
            HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    fun formatMarkValueBig(abbreviation: String?, markScaleValue: Float, markPointsValue: Float): String{
        return when {
            abbreviation != null -> when {
                abbreviation.isNotBlank() -> abbreviation
                markScaleValue > 0f -> "%.1f".format(markScaleValue)
                else -> "?"
            }
            //info.markPointsValue >= 0 && info.markValueMax > 0 ->
            //    "${info.markPointsValue}\n${info.markValueMax}"
            markPointsValue >= 0 -> String.format("%.1f", markPointsValue)
            else -> "Wut?"
        }
    }

    fun formatMarkShortDescription(markValueMax: Float, markPointsValue: Float, countPointsWithoutBase: Boolean?, noCountToAverage: Boolean?, weight: Float): String?{
        return when {
            markValueMax > 0 && markPointsValue >= 0f ->
                if (countPointsWithoutBase == true)
                    String.format("%.1f%% • baza %.1f • poza bazą",
                            markPointsValue / markValueMax * 100f,
                            markValueMax)
                else String.format("%.1f%% • baza %.1f",
                        markPointsValue / markValueMax * 100f,
                        markValueMax)

            markValueMax > 0 -> String.format("baza %.1f", markValueMax)
            weight > 0
                    && noCountToAverage != true
                    && countPointsWithoutBase != true ->
                String.format("Waga %.1f", weight)
            else -> null
        }
    }
}
