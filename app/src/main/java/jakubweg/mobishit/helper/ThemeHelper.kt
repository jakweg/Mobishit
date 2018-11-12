package jakubweg.mobishit.helper

import android.app.Activity
import android.content.Context
import android.support.annotation.StyleRes
import jakubweg.mobishit.R
import java.util.*

object ThemeHelper {

    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"
    private const val THEME_BLACK = "black"
    private const val THEME_AUTO_DARK = "aDark"
    private const val THEME_AUTO_BLACK = "aBlack"

    const val THEME_DEFAULT = THEME_LIGHT

    fun isLightThemeSet(context: Context) = MobiregPreferences.get(context).theme == THEME_LIGHT

    @StyleRes
    fun getBottomSheetTheme(context: Context): Int {
        return when (MobiregPreferences.get(context).theme) {
            THEME_LIGHT -> R.style.BottomSheetDialogLight
            THEME_DARK -> R.style.BottomSheetDialogDark
            THEME_BLACK -> R.style.BottomSheetDialogBlack
            THEME_AUTO_DARK -> if (shouldUseLightTheme()) R.style.BottomSheetDialogLight else R.style.BottomSheetDialogDark
            THEME_AUTO_BLACK -> if (shouldUseLightTheme()) R.style.BottomSheetDialogLight else R.style.BottomSheetDialogBlack
            else -> R.style.BottomSheetDialogLight
        }
    }

    fun makeActivityThemed(activity: Activity?, theme: String?) {
        activity ?: throw NullPointerException()
        if (theme == null)
            return
        activity.setTheme(when (theme) {
            THEME_LIGHT -> return // ignore - default is light
            THEME_DARK -> R.style.AppDarkTheme
            THEME_BLACK -> R.style.AppBlackTheme
            THEME_AUTO_DARK -> if (shouldUseLightTheme()) return else R.style.AppDarkTheme
            THEME_AUTO_BLACK -> if (shouldUseLightTheme()) return else R.style.AppBlackTheme
            else -> return
        })
    }

    private fun shouldUseLightTheme(): Boolean {
        return Calendar.getInstance().run {
            val month = this[Calendar.MONTH]
            val hour = this[Calendar.HOUR_OF_DAY]

            return@run when (month) {
                in Calendar.MARCH..Calendar.SEPTEMBER -> hour in 6..19
                else -> hour in 7..17
            }
        }
    }
}