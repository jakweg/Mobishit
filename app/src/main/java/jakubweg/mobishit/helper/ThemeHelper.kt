package jakubweg.mobishit.helper

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.text.PrecomputedTextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import jakubweg.mobishit.R
import java.util.*

object ThemeHelper {

    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"
    private const val THEME_BLACK = "black"
    private const val THEME_AUTO_DARK = "aDark"
    private const val THEME_AUTO_BLACK = "aBlack"

    const val THEME_DEFAULT = THEME_LIGHT

    fun isLightThemeSet(context: Context) = when (MobiregPreferences.get(context).theme) {
        THEME_LIGHT -> true
        THEME_DARK, THEME_BLACK -> false
        THEME_AUTO_DARK, THEME_AUTO_BLACK -> shouldUseLightTheme()
        else -> false
    }

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

inline var TextView.precomputedText: CharSequence
    get() = throw UnsupportedOperationException()
    set(value) = makePrecomputedText(value)

fun TextView.makePrecomputedText(text: CharSequence) {
    TextViewCompat.setPrecomputedText(this,
            PrecomputedTextCompat.create(text, TextViewCompat.getTextMetricsParams(this)))
}


fun TextView.setTopDrawable(@DrawableRes drawable: Int, @ColorInt color: Int) {
    setCompoundDrawablesWithIntrinsicBounds(null,
            ContextCompat.getDrawable(context, drawable)!!.tintSelf(color),
            null, null)
}

fun TextView.setLeftDrawable(@DrawableRes drawable: Int) {
    setLeftDrawable(drawable, currentTextColor)
}

fun TextView.setLeftDrawable(@DrawableRes drawable: Int, @ColorInt color: Int) {
    setCompoundDrawablesWithIntrinsicBounds(
            AppCompatResources.getDrawable(context, drawable)!!.tintSelf(color),
            null, null, null)
}

fun Drawable.tintSelf(@ColorInt tint: Int): Drawable {
    return DrawableCompat.wrap(this).apply {
        DrawableCompat.setTint(this, tint)
        DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_ATOP)
    }
}


fun Context.themeAttributeToColor(@AttrRes attrColor: Int): Int {
    val outValue = TypedValue()
    val theme = this.theme
    theme.resolveAttribute(
            attrColor, outValue, true)

    return ContextCompat.getColor(this, outValue.resourceId)
}

fun Context.themeAttributeToDrawable(@AttrRes attrDrawable: Int): Drawable? {
    val outValue = TypedValue()
    val theme = this.theme ?: null
    theme?.resolveAttribute(
            attrDrawable, outValue, true)

    return ResourcesCompat.getDrawable(resources, outValue.resourceId, theme)
}

fun invertColor(@ColorInt color: Int): Int {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    return Color.argb(Color.alpha(color), 255 - r, 255 - g, 255 - b)
}

fun View.textView(@IdRes id: Int): TextView? = findViewById<TextView?>(id)

fun View.setText(@IdRes id: Int, text: CharSequence) {
    textView(id)?.precomputedText = text
}

fun NotificationCompat.Builder.setSmallIconCompat(vectorIconId: Int, pngIconId: Int): NotificationCompat.Builder {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        setSmallIcon(pngIconId)
    else
        setSmallIcon(vectorIconId)
    return this
}