package jakubweg.mobishit.fragment

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.averageBy
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.model.asImmutable
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class VirtualMarksFragment : Fragment() {
    companion object {
        fun newInstance() = VirtualMarksFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_virtual_marks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainList = view.findViewById<RecyclerView?>(R.id.markParentsList) ?: return
        val context = mainList.context!!

        val markScaleGroupId = 21
        val markScales = AppDatabase.getAppDatabase(context)
                .markDao.getMarkScalesByGroupId(markScaleGroupId)

        val adapter = VirtualScaleMarksAdapter(context, markScales)
        mainList.adapter = adapter

        view.findViewById<View>(R.id.addNewMark)?.setOnClickListener {
            adapter.createNewMark()
        }
        val weakTitle = WeakReference(view.findViewById<TextView?>(R.id.title))
        adapter.average.observe(this, Observer {
            weakTitle.get()?.apply {
                text = if (it == null || it.isNaN()) {
                    "Tu wyświetlona zostanie twoja średnia"
                } else {
                    String.format("Twoja średnia ważona wynosi %.2f", it)
                }
            }
        })
    }


    private abstract class VirtualMarkListItem {
        abstract var weight: Float
        var adapter = WeakReference<VirtualScaleMarksAdapter>(null)
        abstract fun bindToView(viewHolder: VirtualScaleMarksAdapter.ViewHolder)

        fun changeMarkIndex(newIndex: Int): Int {
            adapter.get()?.lastMarkScaleIndex = newIndex
            adapter.get()?.notifyMarkChanged()
            return newIndex
        }

        fun changeWeight(newWeight: Float): Float {
            adapter.get()?.lastWeight = newWeight
            adapter.get()?.notifyMarkChanged()
            return newWeight
        }
    }

    private class VirtualMarkSingle : VirtualMarkChild(null) {
        private var mWeight = -1f
        override var weight: Float
            get() = mWeight
            set(value) {
                mWeight = value
            }

        override fun bindToView(viewHolder: VirtualScaleMarksAdapter.ViewHolder) {
            super.bindToView(viewHolder)
            if (weight < 0f)
                weight = adapter.get()?.lastWeight ?: 1f
            viewHolder.btnMore?.setOnClickListener {
                adapter.get()?.also { adapter ->
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
            viewHolder.textValue2?.precomputedText = (weight).roundToInt().toString()
        }
    }

    private class VirtualMarkParent : VirtualMarkListItem() {
        private var mWeight = -1f
        override var weight: Float
            get() = mWeight
            set(value) {
                mWeight = value
            }
        var parentType = MarkDao.parentTypesAsText.keyAt(0)
        override fun bindToView(viewHolder: VirtualScaleMarksAdapter.ViewHolder) {
            if (weight < 0f)
                weight = changeWeight(adapter.get()?.lastWeight ?: 1f)

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

            viewHolder.textValue.precomputedText = weight.roundToInt().toString()
            viewHolder.btnAddNewMark?.setOnClickListener {
                adapter.get()?.createNewChildMark(this)
            }
        }
    }

    private open class VirtualMarkChild(
            var parent: VirtualMarkParent?
    ) : VirtualMarkListItem() {
        override var weight: Float
            get() = parent?.weight ?: 0f
            set(value) {
                parent?.weight = value
            }
        var markScaleIndex = -1

        override fun bindToView(viewHolder: VirtualScaleMarksAdapter.ViewHolder) {
            val adapter = adapter.get() ?: return
            if (markScaleIndex == -1) {
                adapter.also { markScaleIndex = changeMarkIndex(it.lastMarkScaleIndex) }
            }

            viewHolder.btnIncrease.setOnClickListener {
                if (markScaleIndex == adapter.markScales.size - 1) return@setOnClickListener
                viewHolder.textValue.precomputedText = adapter.markScalesTitles[changeMarkIndex(++markScaleIndex)]
            }

            viewHolder.btnDecrease.setOnClickListener {
                if (markScaleIndex == 0) return@setOnClickListener
                viewHolder.textValue.precomputedText = adapter.markScalesTitles[changeMarkIndex(--markScaleIndex)]
            }

            viewHolder.textValue.setOnClickListener {
                AlertDialog.Builder(adapter.context)
                        .setTitle("Wybierz ocenę")
                        .setNegativeButton("Anuluj", null)
                        .setItems(adapter.markScalesTitles) { _, pos ->
                            markScaleIndex = changeMarkIndex(pos)
                            viewHolder.textValue.precomputedText = adapter.markScalesTitles[markScaleIndex]
                        }
                        .show()
            }

            viewHolder.textValue.precomputedText = adapter.markScalesTitles[markScaleIndex]
        }
    }


    private class VirtualScaleMarksAdapter(
            val context: Context,
            val markScales: List<MarkDao.MarkScaleShortInfo>)
        : RecyclerView.Adapter<VirtualScaleMarksAdapter.ViewHolder>() {

        val markScalesTitles = Array(markScales.size) { markScales[it].abbreviation }
        var lastWeight = 1f
        var lastMarkScaleIndex = markScalesTitles.size.div(2)

        private val mAverageData = MutableLiveData<Float>().apply { value = Float.NaN }
        val average = mAverageData.asImmutable

        companion object {
            private const val TYPE_PARENT = 0
            private const val TYPE_CHILD = 1
            private const val TYPE_SINGLE = 2
        }

        private fun calculateAverageScaleMarks(): Float {
            var totalSum = 0f
            var totalWeight = 0f
            mainList.filterIsInstance<VirtualMarkChild>()
                    .groupBy { it.parent }
                    .filterNot { it.value.isEmpty() }
                    .forEach { entry ->
                        val key = entry.key
                        // no parent
                        if (key == null || key.parentType == MarkDao.PARENT_TYPE_COUNT_EVERY) {
                            entry.value.forEach {
                                totalSum += markScales[it.markScaleIndex].markValue * it.weight
                                totalWeight += it.weight
                            }
                            return@forEach
                        }

                        when (key.parentType) {
                            MarkDao.PARENT_TYPE_COUNT_AVERAGE -> {
                                totalSum += entry.value.averageBy {
                                    markScales[it.markScaleIndex].markValue.toDouble()
                                }.toFloat() * key.weight
                                totalWeight += key.weight
                            }
                            MarkDao.PARENT_TYPE_COUNT_BEST -> {
                                totalSum += markScales[entry.value
                                        .maxBy { markScales[it.markScaleIndex].markValue }
                                !!.markScaleIndex].markValue * key.weight
                                totalWeight += key.weight
                            }
                            MarkDao.PARENT_TYPE_COUNT_LAST -> {
                                totalSum += markScales[entry.value
                                        .last().markScaleIndex].markValue * key.weight
                                totalWeight += key.weight
                            }
                            MarkDao.PARENT_TYPE_COUNT_WORSE -> {
                                totalSum += markScales[entry.value
                                        .minBy { markScales[it.markScaleIndex].markValue }
                                !!.markScaleIndex].markValue * key.weight
                                totalWeight += key.weight
                            }
                        }
                    }
            return totalSum / totalWeight
        }

        fun notifyMarkChanged() {
            Handler(Looper.getMainLooper()).post(updateAverageRunnable)
        }

        private val updateAverageRunnable = Runnable {
            mAverageData.value = calculateAverageScaleMarks()
        }

        private val mainList = mutableListOf<VirtualMarkListItem>()
        private val inflater = LayoutInflater.from(context)!!

        fun createNewMark() {
            insertMark(VirtualMarkSingle(), 0)
        }

        fun replaceSingle(pos: Int) {
            val item = mainList[pos] as VirtualMarkSingle
            mainList.removeAt(pos)

            val new = VirtualMarkParent()
            new.adapter = WeakReference(this)
            mainList.add(pos, new)
            notifyItemChanged(pos)

            insertMark(VirtualMarkChild(new).also {
                it.markScaleIndex = item.markScaleIndex
                new.weight = item.weight
            }, pos + 1)

            notifyMarkChanged()
        }

        fun createNewChildMark(parent: VirtualMarkParent) {
            insertMark(VirtualMarkChild(parent),
                    mainList.indexOf(parent) + 1)

            notifyMarkChanged()
        }

        private fun insertMark(mark: VirtualMarkListItem, pos: Int) {
            mark.adapter = WeakReference(this)
            mainList.add(pos, mark)
            notifyItemInserted(pos)
        }

        fun deleteMarkAt(pos: Int) {
            notifyMarkChanged()
            val iterator = mainList.iterator()
            for (i in 0 until pos)
                iterator.next()
            val item = iterator.next()
            iterator.remove()
            if (item !is VirtualMarkParent) {
                notifyItemRemoved(pos)
                return
            }

            var removed = 1
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it !is VirtualMarkChild || it.parent != item)
                    break
                iterator.remove()
                removed++
            }

            notifyItemRangeRemoved(pos, removed)
        }

        override fun getItemCount() = mainList.size

        override fun getItemViewType(position: Int): Int {
            return mainList[position].run {
                when (this) {
                    is VirtualMarkSingle -> TYPE_SINGLE
                    is VirtualMarkChild -> TYPE_CHILD
                    is VirtualMarkParent -> TYPE_PARENT
                    else -> throw IllegalArgumentException()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(when (type) {
                TYPE_SINGLE -> R.layout.mark_calculator_single_list_item
                TYPE_CHILD -> R.layout.mark_calculator_child_list_item
                TYPE_PARENT -> R.layout.mark_calculator_parent_list_item
                else -> throw IllegalStateException()
            }, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            val item = mainList[pos]
            item.bindToView(holder)
        }


        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val btnIncrease = v.findViewById<View>(R.id.btnIncrease)!!
            val btnDecrease = v.findViewById<View>(R.id.btnDecrease)!!

            val btnIncrease2 = v.findViewById<View?>(R.id.btnIncrease2)
            val btnDecrease2 = v.findViewById<View?>(R.id.btnDecrease2)
            val textValue2 = v.findViewById<TextView?>(R.id.markValue2)

            val btnMore = v.findViewById<View?>(R.id.btnMore)

            val btnDelete = v.findViewById<View>(R.id.btnDeleteMark)!!
            val btnAddNewMark = v.findViewById<View?>(R.id.btnAddNewMark)
            val textParentType = v.findViewById<TextView?>(R.id.parentTypeText)
            val textValue = v.findViewById<TextView>(R.id.markValue)!!

            init {
                btnDelete.setOnClickListener { deleteMarkAt(adapterPosition) }
            }
        }
    }
}
