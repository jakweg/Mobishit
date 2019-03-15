package jakubweg.mobishit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R

class MarksListView : View {
    constructor(context: Context) : super(context) {
        //init(null, 0)
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        //init(attrs, 0)
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        //init(attrs, defStyle)
        init()
    }

    private fun init(/*attrs: AttributeSet?, defStyle: Int*/) {
        if (BuildConfig.DEBUG && isInEditMode) {
            setDisplayedMarks(listOf("5", "4+", "3-", "nb", "4", "5", "4+", "3-", "nb", "4", "5", "4+", "3-", "nb",
                    "4", "5", "4+", "3-", "nb", "4", "5", "4+", "3-", "nb", "4"))
        }
    }

    fun setDisplayedMarks(marks: Collection<String?>) {
        mTempMarks = marks
        needsRecalculation = true
        setWillNotDraw(false)
        invalidate()
    }

    private var needsRecalculation = true
    private var mTempMarks: Collection<String?> = listOf()

    private var mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var mRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private class DrawPosition(var x: Float, var y: Float, var text: String)

    private var mPositionsToDraw = arrayListOf<DrawPosition>()
    private var mRoundCornersSize = 0f
    private var mEllipsisWidth = 0f
    private var mEllipsisLeft = 0f //if not 0 then draw

    private fun calculateDrawings() {
        val marks = mTempMarks
        val width = this.width.toFloat()
        val res = context?.resources ?: return
        val context = this.context ?: return
        val backgroundColor = context.getColorFromAttr(R.attr.backgroundSecondaryColor)
        val textColor = ColorUtils.setAlphaComponent(context.themeAttributeToColor(android.R.attr.textColorPrimary), 185)
        val separatorWidth = res.getDimension(R.dimen.separatorWithMarkList)
        val textSize = res.getDimensionPixelSize(R.dimen.textSizeMarkList).toFloat()
        mRoundCornersSize = res.getDimension(R.dimen.roundSizeMarkList)

        mTextPaint.color = textColor
        mTextPaint.textSize = textSize
        mTextPaint.textAlign = Paint.Align.CENTER

        mRectPaint.color = backgroundColor

        mEllipsisWidth = mTextPaint.measureText(" … ")
        mEllipsisLeft = 0f
        val positionsToDraw = ArrayList<DrawPosition>(marks.size)

        var left = 0f
        for (it in marks) {
            if (it == null) continue
            val textWidth = mTextPaint.measureText(" $it ")

            if (left + textWidth > width) {
                while (left + mEllipsisWidth > width && positionsToDraw.isNotEmpty()) {
                    left = positionsToDraw.last().x
                    positionsToDraw.removeAt(positionsToDraw.size - 1)
                }
                mEllipsisLeft = left
                break
            }

            positionsToDraw.add(DrawPosition(left, textWidth, it))
            left += textWidth + separatorWidth
        }

        mPositionsToDraw = positionsToDraw
        needsRecalculation = false
    }


    @ColorInt
    fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    override fun onDraw(canvas: Canvas) {
        if (needsRecalculation)
            calculateDrawings()

        val height = this.height.toFloat()
        val textSize = mTextPaint.textSize

        // for dark theme, because it's not visible anyway
        val isDrawingRectEnabled = mRectPaint.color != Color.BLACK

        mPositionsToDraw.forEach {
            if (isDrawingRectEnabled)
                canvas.drawRoundRectCompat(it.x, 0f,
                        it.x + it.y,
                        height, mRoundCornersSize, mRoundCornersSize, mRectPaint)
            canvas.drawText(it.text, it.x + it.y / 2f, textSize, mTextPaint)
        }

        if (mEllipsisLeft != 0f) {
            if (isDrawingRectEnabled)
                canvas.drawRoundRectCompat(mEllipsisLeft, 0f,
                        mEllipsisLeft + mEllipsisWidth,
                        height, mRoundCornersSize, mRoundCornersSize, mRectPaint)
            canvas.drawText(" … ", mEllipsisLeft + mEllipsisWidth / 2f, textSize, mTextPaint)
        }
    }


    @ColorInt
    private fun Context.getColorFromAttr(
            @AttrRes attrColor: Int,
            typedValue: TypedValue = TypedValue(),
            resolveRefs: Boolean = true
    ): Int {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    private fun Context.themeAttributeToColor(@AttrRes attrColor: Int): Int {
        val outValue = TypedValue()
        val theme = this.theme
        theme.resolveAttribute(
                attrColor, outValue, true)

        return ContextCompat.getColor(
                this, outValue.resourceId)
    }

    private fun Canvas.drawRoundRectCompat(left: Float, top: Float,
                                           right: Float, bottom: Float,
                                           rx: Float, ry: Float,
                                           paint: Paint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawRoundRect(left, top, right, bottom, rx, ry, paint)
        } else {
            drawRect(left, top, right, bottom, paint)
        }
    }

}
