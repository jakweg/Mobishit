package jakubweg.mobishit.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R


class AttendanceBarView : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        context!!.apply {
            mCornerRadius = resources!!.getDimension(R.dimen.attendance_bar_corners)
        }

        if (BuildConfig.DEBUG || isInEditMode)
            setAttendanceData(249, 74, 23)
    }

    // color, count
    private var attendancesList = listOf<Pair<Int, Int>>()
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setAttendanceData(presentCount: Int, absentCount: Int, lateCount: Int) {
        assert(presentCount >= 0 && absentCount >= 0 && lateCount >= 0)
        setAttendanceData(listOf(
                -16751104 to presentCount,
                -65536 to absentCount,
                -26368 to lateCount
        ))
    }

    fun setAttendanceData(list: List<Pair<Int, Int>>) {
        attendancesList = list
        setWillNotDraw(false)
        invalidate()
    }

    private var mPath = Path()
    private var mCornerRadius: Float = 0.toFloat()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val r = RectF(0f, 0f, w.toFloat(), h.toFloat())
        mPath = Path().apply {
            addRoundRect(r, mCornerRadius, mCornerRadius, Path.Direction.CW)
            close()
        }
    }

    @SuppressLint("DrawAllocation", "CanvasSize")
    override fun onDraw(canvas: Canvas?) {
        canvas ?: return

        val sum = attendancesList.sumBy { it.second }
        if (sum == 0) {
            return
        }

        canvas.clipPath(mPath)

        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        var left = paddingLeft.toFloat()
        val unitWidth = (width - paddingRight - paddingLeft).toFloat() / sum.toFloat()

        for ((color, count) in attendancesList) {
            if (count == 0)
                continue

            val width = unitWidth * count
            mainPaint.color = color
            canvas.drawRect(left, top, left + width, bottom, mainPaint)
            left += width
        }
    }
}
