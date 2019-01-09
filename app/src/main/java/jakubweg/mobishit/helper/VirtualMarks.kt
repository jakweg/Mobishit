package jakubweg.mobishit.helper

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.db.VirtualMarkEntity
import jakubweg.mobishit.fragment.VirtualMarksFragment
import jakubweg.mobishit.fragment.VirtualMarksFragment.VirtualBaseAdapter.Companion.TYPE_POINTS_SINGLE
import jakubweg.mobishit.fragment.VirtualMarksFragment.VirtualBaseAdapter.Companion.TYPE_SCALE_CHILD
import jakubweg.mobishit.fragment.VirtualMarksFragment.VirtualBaseAdapter.Companion.TYPE_SCALE_PARENT
import jakubweg.mobishit.fragment.VirtualMarksFragment.VirtualBaseAdapter.Companion.TYPE_SCALE_SINGLE
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.roundToInt


abstract class VirtualMarkListItem {
    companion object {
        fun CharSequence.toFloatLocaleCompat() = toString().replace(',', '.').toFloatOrNull()

        interface CallbackFloat {
            fun call(value: Float)
        }

        inline fun askForNumber(context: Context, default: Float,
                                title: CharSequence, crossinline listener: (Float) -> Unit) {
            askForNumber(context, default, title, object : CallbackFloat {
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
                        val value = textView.text?.toString()?.toFloatOrNull()
                                ?: return@setPositiveButton
                        callback.call(value)
                    }
                    .create().apply {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                        textView.requestFocus()
                    }
                    .show()
        }
    }


    abstract var weight: Float
    var adapter = WeakReference<VirtualMarksFragment.VirtualBaseAdapter>(null)

    protected val markScalesAdapter
        get() = adapter.get() as? VirtualMarksFragment.VirtualScaleMarksAdapter

    protected val markPointsAdapter
        get() = adapter.get() as? VirtualMarksFragment.VirtualMarkPointsAdapter

    abstract val type: Int

    abstract val asEntity: VirtualMarkEntity

    abstract fun bindToView(viewHolder: VirtualMarksFragment.VirtualMarkViewHolder)

    fun changeMarkIndex(newIndex: Int): Int {
        markScalesAdapter?.also {
            it.lastMarkScaleIndex = newIndex
            it.notifyMarkChanged()
        }
        return newIndex
    }

    fun changeWeight(newWeight: Float): Float {
        markScalesAdapter?.also {
            it.lastWeight = newWeight
            it.notifyMarkChanged()
        }
        weight = newWeight
        return newWeight
    }
}

class VirtualMarkSingle : VirtualMarkChild(null) {
    private var mWeight = -1f
    override var weight: Float
        get() = mWeight
        set(value) {
            mWeight = value
        }
    override val type get() = TYPE_SCALE_SINGLE

    override val asEntity: VirtualMarkEntity
        get() = VirtualMarkEntity(0, type, markScaleIndex.toFloat(), weight)

    override fun bindToView(viewHolder: VirtualMarksFragment.VirtualMarkViewHolder) {
        super.bindToView(viewHolder)
        if (weight < 0f)
            weight = markScalesAdapter?.lastWeight ?: 1f
        viewHolder.btnMore?.setOnClickListener {
            markScalesAdapter?.also { adapter ->
                adapter.replaceSingle(viewHolder.adapterPosition)
            }
        }

        viewHolder.btnIncrease2?.setOnClickListener {
            viewHolder.textValue2?.precomputedText = changeWeight(++weight).roundToInt().toString()
        }
        viewHolder.btnDecrease2?.setOnClickListener {
            if (weight <= 1f) return@setOnClickListener
            viewHolder.textValue2?.precomputedText = changeWeight(--weight).roundToInt().toString()
        }

        viewHolder.textValue2?.setOnClickListener {
            askForNumber(it.context!!, (it as TextView).text.toFloatLocaleCompat() ?: 1f,
                    "Podaj wagę tej oceny") { `val` ->
                viewHolder.textValue2.precomputedText = changeWeight(`val`).roundToInt().toString()
            }
        }

        viewHolder.textValue2?.precomputedText = (weight).roundToInt().toString()
    }
}

class VirtualMarkParent : VirtualMarkListItem() {
    private var mWeight = -1f
    override var weight: Float
        get() = mWeight
        set(value) {
            mWeight = value
        }

    override val type get() = TYPE_SCALE_PARENT

    override val asEntity: VirtualMarkEntity
        get() = VirtualMarkEntity(0, type, parentType.toFloat(), mWeight)

    var parentType = MarkDao.PARENT_TYPE_COUNT_AVERAGE
    override fun bindToView(viewHolder: VirtualMarksFragment.VirtualMarkViewHolder) {
        if (weight < 0f)
            weight = markScalesAdapter?.lastWeight ?: 1f

        viewHolder.textParentType?.apply {
            precomputedText = MarkDao.parentTypesAsText[parentType]
            setOnClickListener { view ->

                val size = MarkDao.parentTypesAsText.size()
                val keys = IntArray(size) { MarkDao.parentTypesAsText.keyAt(it) }
                val values = Array<String>(size) { MarkDao.parentTypesAsText.valueAt(it) }

                AlertDialog.Builder(view.context ?: return@setOnClickListener)
                        .setTitle("Wybierz typ poprawy")
                        .setNegativeButton("Anuluj", null)
                        .setItems(values) { _, pos ->
                            parentType = keys[pos]
                            precomputedText = values[pos]
                            adapter.get()?.notifyMarkChanged()
                        }
                        .show()
            }
        }

        viewHolder.btnIncrease.setOnClickListener {
            viewHolder.textValue.precomputedText = changeWeight(++weight).roundToInt().toString()
        }
        viewHolder.btnDecrease.setOnClickListener {
            if (weight <= 1f)
                return@setOnClickListener
            viewHolder.textValue.precomputedText = changeWeight(--weight).roundToInt().toString()
        }

        viewHolder.textValue.setOnClickListener {
            askForNumber(it.context!!, (it as TextView).text.toFloatLocaleCompat() ?: 1f,
                    "Podaj wagę tej oceny") { `val` ->
                viewHolder.textValue.precomputedText = changeWeight(`val`).roundToInt().toString()
            }
        }
        viewHolder.textValue.precomputedText = weight.roundToInt().toString()
        viewHolder.btnAddNewMark?.setOnClickListener {
            markScalesAdapter?.createNewChildMark(this)
        }
    }
}

open class VirtualMarkChild(
        var parent: VirtualMarkParent?
) : VirtualMarkListItem() {
    override var weight: Float
        get() = parent?.weight ?: 0f
        set(value) {
            parent?.weight = value
        }
    override val type get() = TYPE_SCALE_CHILD

    override val asEntity: VirtualMarkEntity
        get() = VirtualMarkEntity(0, type, markScaleIndex.toFloat(), 0f)
    var markScaleIndex = -1

    override fun bindToView(viewHolder: VirtualMarksFragment.VirtualMarkViewHolder) {
        val adapter = adapter.get() ?: parent?.adapter?.get() ?: return
        if (markScaleIndex < 0) {
            markScalesAdapter?.also { markScaleIndex = changeMarkIndex(it.lastMarkScaleIndex) }
            if (markScaleIndex < 0) markScaleIndex = 0
        }

        viewHolder.btnIncrease.setOnClickListener {
            markScalesAdapter?.also { adapter ->
                if (adapter.markScales.size == markScaleIndex + 1) return@setOnClickListener
                viewHolder.textValue.precomputedText = adapter.markScalesTitles[changeMarkIndex(++markScaleIndex)]
            }
        }

        viewHolder.btnDecrease.setOnClickListener {
            if (markScaleIndex == 0) return@setOnClickListener
            viewHolder.textValue.precomputedText = markScalesAdapter?.markScalesTitles?.get(changeMarkIndex(--markScaleIndex)) ?: ""
        }

        viewHolder.textValue.setOnClickListener {
            val titles = markScalesAdapter?.markScalesTitles ?: return@setOnClickListener
            AlertDialog.Builder(adapter.context)
                    .setTitle("Wybierz ocenę")
                    .setNegativeButton("Anuluj", null)
                    .setItems(titles) { _, pos ->
                        markScaleIndex = changeMarkIndex(pos)
                        viewHolder.textValue.precomputedText = titles[markScaleIndex]
                    }
                    .show()
        }

        markScalesAdapter?.markScalesTitles?.get(markScaleIndex)?.also {
            viewHolder.textValue.precomputedText = it
        }
    }
}


class VirtualPointsMark : VirtualMarkListItem() {
    var gotPointsSum = 0f
    var basePointsSum = 10f
    override val type get() = TYPE_POINTS_SINGLE
    private val locale = Locale.getDefault()

    override val asEntity: VirtualMarkEntity
        get() = VirtualMarkEntity(0, type, gotPointsSum, basePointsSum)

    override var weight: Float
        get() = basePointsSum
        set(value) {
            basePointsSum = value
        }

    override fun bindToView(viewHolder: VirtualMarksFragment.VirtualMarkViewHolder) {
        viewHolder.title1?.precomputedText = "Zdobyte punkty:"
        viewHolder.title2?.precomputedText = "Baza:"

        viewHolder.btnIncrease.setOnClickListener {
            gotPointsSum++
            updateTitles(viewHolder)
        }
        viewHolder.btnDecrease.setOnClickListener {
            if (gotPointsSum <= 0f) return@setOnClickListener
            gotPointsSum--
            updateTitles(viewHolder)
        }

        viewHolder.btnIncrease2?.setOnClickListener {
            basePointsSum++
            updateTitles(viewHolder)
        }
        viewHolder.btnDecrease2?.setOnClickListener {
            if (basePointsSum <= 1f) return@setOnClickListener
            basePointsSum--
            updateTitles(viewHolder)
        }

        viewHolder.textValue.setOnClickListener {
            VirtualMarkListItem.askForNumber(it.context!!, (it as? TextView?)?.text
                    ?.toFloatLocaleCompat() ?: 0f,
                    "Podaj ilość zdobytych punktów") { value ->
                gotPointsSum = value
                updateTitles(viewHolder)
            }
        }
        viewHolder.textValue2?.setOnClickListener {
            VirtualMarkListItem.askForNumber(it.context!!, (it as? TextView?)?.text
                    ?.toFloatLocaleCompat() ?: 0f,
                    "Podaj ilość maksymalnych punktów") { value ->
                basePointsSum = value
                updateTitles(viewHolder)
            }
        }
        updateTitles(viewHolder)
    }

    private fun updateTitles(holder: VirtualMarksFragment.VirtualMarkViewHolder) {
        holder.textValue.precomputedText = "%.1f".format(locale, gotPointsSum)
        holder.textValue2?.precomputedText = "%.1f".format(locale, basePointsSum)
        adapter.get()?.notifyMarkChanged()
    }
}