import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_POINTS_SINGLE
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_CHILD
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_PARENT
import VirtualMarksFragment.VirtualMarksAdapter.Companion.TYPE_SCALE_SINGLE
import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.util.SparseArray
import android.view.WindowManager
import android.widget.EditText
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.db.VirtualMarkEntity
import jakubweg.mobishit.helper.SimpleCallback
import jakubweg.mobishit.helper.makeCallback
import jakubweg.mobishit.helper.oneDigitAfterDot
import jakubweg.mobishit.helper.precomputedText
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt

abstract class VirtualMarkBase(val type: Int)
    : VirtualMarksFragment.VirtualMarksChangeListener() {

    companion object {
        private fun CharSequence.toFloatLocaleCompat() = toString().replace(',', '.').toFloatOrNull()

        interface CallbackFloat {
            fun call(value: Float)
        }

        inline fun askForNumber(m: VirtualMarkBase, default: Float,
                                title: CharSequence, crossinline listener: (Float) -> Unit) {
            askForNumber(m.adapter.get()!!.context, default, title, object : CallbackFloat {
                override fun call(value: Float) = listener.invoke(value)
            })
        }

        @SuppressLint("SetTextI18n")
        fun askForNumber(context: Context, default: Float, title: CharSequence, callback: CallbackFloat) {
            val textView = EditText(context)
            textView.setText(
                    if (default.roundToInt().toFloat() == default) default.roundToInt().toString()
                    else "%.1f".format(Locale.US, default))
            textView.hint = "Wartość"
            textView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            AlertDialog.Builder(context)
                    .setView(textView)
                    .setTitle(title)
                    .setNegativeButton("Anuluj", null)
                    .setPositiveButton("Zapisz") { _, _ ->
                        val value = textView.text?.toFloatLocaleCompat()
                                ?: return@setPositiveButton
                        if (value < 1000)
                            callback.call(value)
                    }
                    .create().apply {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                        textView.requestFocus()
                    }
                    .show()
        }

        fun askForMark(context: Context, title: CharSequence,
                       fragment: VirtualMarksFragment.VirtualMarksAdapter, selectedIndex: Int,
                       callback: SimpleCallback<Int>) {
            AlertDialog.Builder(context)
                    .setTitle(title)
                    .setSingleChoiceItems(fragment.markScalesFiltered, selectedIndex) { d, which ->
                        d.dismiss()
                        callback.call(fragment.markScales.indexOfFirst { fragment.markScalesFiltered[which] === it.abbreviation })
                    }
                    .show()
        }

        fun askByList(context: Context, title: CharSequence,
                      list: Array<String>, selectedIndex: Int,
                      callback: SimpleCallback<Int>) {
            AlertDialog.Builder(context)
                    .setTitle(title)
                    .setSingleChoiceItems(list, selectedIndex) { d, which ->
                        d.dismiss()
                        callback.call(which)
                    }
                    .show()
        }
    }

    override fun onDeleteClicked() {
        adapter.get()?.onDeleteRequested(currentViewHolder.get()?.adapterPosition ?: return)
    }

    var adapter = WeakReference<VirtualMarksFragment.VirtualMarksAdapter?>(null)
    var currentViewHolder = WeakReference<VirtualMarksFragment.VirtualMarkViewHolder?>(null)

    protected fun notifyDataChanged() {
        applyToViews()
        adapter.get()!!.requestAverageRecalculation()
    }

    protected fun notifyLastScaleIndex(index: Int) {
        adapter.get()!!.apply {
            lastScaleIndex = index
            applyToViews()
            requestAverageRecalculation()
        }
    }

    protected fun notifyWeight(value: Float) {
        adapter.get()!!.apply {
            lastWeight = value
            applyToViews()
            requestAverageRecalculation()
        }
    }

    abstract fun applyToViews()
    abstract fun toDatabaseEntity(markScales: List<MarkDao.MarkScaleShortInfo>): VirtualMarkEntity
}

class VirtualMarkPoints(
        var pointsValue: Float,
        var baseValue: Float
) : VirtualMarkBase(TYPE_POINTS_SINGLE) {

    override fun toDatabaseEntity(markScales: List<MarkDao.MarkScaleShortInfo>) = VirtualMarkEntity(0, TYPE_POINTS_SINGLE, pointsValue, baseValue)

    override fun applyToViews() {
        currentViewHolder.get()?.also {
            it.markValue1.precomputedText = pointsValue.oneDigitAfterDot
            it.markValue2!!.precomputedText = baseValue.oneDigitAfterDot
        }
    }

    override fun onFirstRowClicked(valueChange: Byte): Boolean {
        if (valueChange == 0.toByte()) {
            askForNumber(this, pointsValue, "Ilość zdobytych punktów") {
                pointsValue = it
                notifyDataChanged()
            }
        } else {
            pointsValue = Math.max(pointsValue + valueChange.toFloat(), 0f)
            notifyDataChanged()
        }
        return true
    }

    override fun onSecondRowClicked(valueChange: Byte) {
        if (valueChange == 0.toByte()) {
            askForNumber(this, baseValue, "Bazowa ilość punktów") {
                baseValue = it
                notifyDataChanged()
            }
        } else {
            baseValue = Math.max(baseValue + valueChange.toFloat(), 0f)
            notifyDataChanged()
        }
    }
}

class VirtualMarkScaleSingle(
        var scaleIndex: Int,
        var weight: Float
) : VirtualMarkBase(TYPE_SCALE_SINGLE) {

    override fun toDatabaseEntity(markScales: List<MarkDao.MarkScaleShortInfo>) = VirtualMarkEntity(0, TYPE_SCALE_SINGLE, markScales[scaleIndex].id.toFloat(), weight)

    override fun applyToViews() {
        currentViewHolder.get()?.also {
            adapter.get()?.apply {
                it.markValue1.precomputedText = markScales[scaleIndex].abbreviation
                it.markValue2!!.precomputedText = weight.oneDigitAfterDot
            }
        }
    }

    override fun onFirstRowClicked(valueChange: Byte): Boolean {
        if (valueChange == 0.toByte()) {
            adapter.get()!!.apply {
                askForMark(context, "Wybierz ocenę", this, scaleIndex, makeCallback {
                    scaleIndex = it
                    notifyLastScaleIndex(scaleIndex)
                })
            }
            return true
        } else {
            adapter.get()?.markScales?.apply {
                if (scaleIndex + valueChange in 0 until size) {
                    scaleIndex += valueChange
                    if (get(scaleIndex).selectable) {
                        notifyLastScaleIndex(scaleIndex)
                        return true
                    } else {
                        if (!onFirstRowClicked(valueChange))
                            scaleIndex -= valueChange
                    }
                }
            }
        }
        return false
    }

    override fun onSecondRowClicked(valueChange: Byte) {
        if (valueChange == 0.toByte()) {
            askForNumber(this, weight, "Waga") {
                weight = it
                notifyWeight(weight)
            }
        } else {
            weight = Math.max(weight + valueChange.toFloat(), 1f)
            notifyWeight(weight)
        }
    }

    override fun onExpandClicked() {
        currentViewHolder.get()?.also {
            adapter.get()?.onExpandScaleMarkRequested(it.adapterPosition)
        }
    }
}

class VirtualMarkParent(var parentType: Int,
                        var weight: Float) : VirtualMarkBase(TYPE_SCALE_PARENT) {

    override fun toDatabaseEntity(markScales: List<MarkDao.MarkScaleShortInfo>) = VirtualMarkEntity(0, TYPE_SCALE_PARENT, parentType.toFloat(), weight)

    override fun applyToViews() {
        currentViewHolder.get()?.also {
            it.markValue1.precomputedText = weight.oneDigitAfterDot
            it.parentTypeText!!.precomputedText = MarkDao.parentTypesAsText[parentType]
        }
    }

    override fun onParentTypeTextClicked() {
        val types = MarkDao.parentTypesAsText.valuesToArray()
        val selected = MarkDao.parentTypesAsText.indexOfKey(parentType)

        askByList(adapter.get()!!.context, "Wybierz typ liczenia poprawy",
                types, selected, makeCallback {
            val indexOfValue = MarkDao.parentTypesAsText.indexOfValue(types[it])
            parentType = MarkDao.parentTypesAsText.keyAt(indexOfValue)
            notifyDataChanged()
        })

    }

    override fun onFirstRowClicked(valueChange: Byte): Boolean {
        if (valueChange == 0.toByte()) {
            askForNumber(this, weight, "Waga") {
                weight = it
                notifyDataChanged()
            }
        } else {
            weight = Math.max(weight + valueChange.toFloat(), 1f)
            notifyWeight(weight)
        }
        return true
    }

    override fun onAddMarkClicked() {
        adapter.get()?.onAddChildRequested(
                currentViewHolder.get()?.adapterPosition ?: return)
    }
}

private inline fun <reified E> SparseArray<E>.valuesToArray() = Array<E>(size()) { valueAt(it) }

class VirtualMarkChild(var scaleIndex: Int) : VirtualMarkBase(TYPE_SCALE_CHILD) {
    override fun toDatabaseEntity(markScales: List<MarkDao.MarkScaleShortInfo>) = VirtualMarkEntity(0, TYPE_SCALE_CHILD,
            markScales[scaleIndex].id.toFloat(), 0f)

    override fun applyToViews() {
        currentViewHolder.get()?.also {
            adapter.get()?.apply {
                it.markValue1.precomputedText = markScales[scaleIndex].abbreviation
            }
        }
    }

    override fun onFirstRowClicked(valueChange: Byte): Boolean {
        if (valueChange == 0.toByte()) {
            adapter.get()!!.apply {
                askForMark(context, "Wybierz ocenę", this, scaleIndex, makeCallback {
                    scaleIndex = it
                    notifyLastScaleIndex(scaleIndex)
                })
            }
            return true
        } else {
            adapter.get()?.markScales?.apply {
                if (scaleIndex + valueChange in 0 until size) {
                    scaleIndex += valueChange
                    if (get(scaleIndex).selectable) {
                        notifyLastScaleIndex(scaleIndex)
                        return true
                    } else {
                        if (!onFirstRowClicked(valueChange))
                            scaleIndex -= valueChange
                    }
                }
            }
        }
        return false
    }


    /*if (scaleIndex + valueChange in 0 until size) {
        if (!get(scaleIndex).selectable)
            onFirstRowClicked(valueChange)
        else {
            scaleIndex += valueChange
            notifyLastScaleIndex(scaleIndex)
        }
    }*/
}

