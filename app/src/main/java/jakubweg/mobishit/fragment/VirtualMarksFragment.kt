package jakubweg.mobishit.fragment

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.MainThread
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.averageBy
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.State
import jakubweg.mobishit.model.VirtualMarksModel
import jakubweg.mobishit.model.asImmutable
import java.lang.ref.WeakReference

class VirtualMarksFragment : Fragment() {
    companion object {
        fun newInstance() = newInstance(NO_METHOD, -1, -1, Long.MIN_VALUE, Long.MAX_VALUE)

        // launch methods
        private const val NO_METHOD = 0
        private const val METHOD_SELECTED_MARK_SCALE_GROUPS = 2
        private const val METHOD_POINTS = 3
        private const val METHOD_IMPORT_FROM_SUBJECT = 4
        private const val METHOD_IMPORTED = 5 //don't do anything

        fun newInstance(method: Int,
                        markScaleGroupId: Int,
                        subjectId: Int,
                        start: Long,
                        end: Long) = VirtualMarksFragment().apply {
            arguments = Bundle(4).also {
                it.putInt("method", method)
                it.putInt("markScaleGroupId", markScaleGroupId)
                it.putInt("subjectId", subjectId)
                it.putLong("start", start)
                it.putLong("end", end)
            }
        }
    }

    val viewModel get() = ViewModelProviders.of(this)[VirtualMarksModel::class.java]

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentId = if ((arguments?.getInt("method", NO_METHOD) ?: NO_METHOD) == NO_METHOD)
            R.layout.fragment_about_virtual_marks else R.layout.fragment_virtual_marks
        return inflater.inflate(fragmentId, container, false)
    }

    private fun onMethodSelected(view: View) {
        when (view.id) {
            R.id.cardImport -> viewModel.requestState(State.CHOOSE_SUBJECT)
            R.id.cardMarkScales -> viewModel.requestState(State.CHOOSE_SCALE_GROUP)
            R.id.cardPoints -> viewModel.requestState(State.PREPARE_FOR_POINTS_MARKS)
        }
    }

    private interface OnAnySelected<T> {
        fun onSelected(item: T)
    }

    private inline fun <T> askUsingDialog(items: List<T>, title: String,
                                          allowSkippingWhenOneElement: Boolean,
                                          crossinline listener: (T) -> Unit) {
        askUsingDialog(items, title, allowSkippingWhenOneElement, object : OnAnySelected<T> {
            override fun onSelected(item: T) {
                listener.invoke(item)
            }
        })
    }

    private fun <T> askUsingDialog(items: List<T>, title: String,
                                   allowSkippingWhenOneElement: Boolean,
                                   listener: OnAnySelected<T>) {
        if (allowSkippingWhenOneElement && items.size == 1) {
            listener.onSelected(items.first())
            return
        }
        AlertDialog.Builder(context ?: return)
                .setTitle(title)
                .setItems(Array(items.size) { items[it].toString() }) { _, pos ->
                    listener.onSelected(items[pos])
                }
                .setNegativeButton("Anuluj") { di, _ -> di.cancel() }
                .setOnCancelListener {
                    viewModel.requestState(State.NOTHING)
                }
                .show()
    }

    private fun recreate() {
        activity?.supportFragmentManager
                ?.beginTransaction()
                ?.detach(this)
                ?.attach(this)
                ?.commitAllowingStateLoss()
    }

    private fun onMarkScaleGroupsLoaded() {
        askUsingDialog(viewModel.markScaleGroups, if (viewModel.subjectId <= 0) "Wybierz typ ocen" else
            "Wybierz skalę z której mają być zaimportowane oceny",
                true) {
            viewModel.apply {
                markScaleGroupId = it.id
                if (subjectId > 0)
                    requestState(State.CHOOSE_TERM)
                else
                    requestState(State.PREPARE_FOR_SCALE_MARKS)
            }
        }
    }

    private fun onSubjectsToImportLoaded() {
        askUsingDialog(viewModel.subjects,
                "Wybierz przedmiot z którego chcesz zaimportować oceny",
                false) {
            viewModel.apply {
                subjectId = it.id
                requestState(State.CHOOSE_SCALE_GROUP)
            }
        }
    }

    private fun onTermsLoaded() {
        askUsingDialog(viewModel.terms, "Wybierz semestr z którego zaimportować oceny",
                true) {
            viewModel.apply {
                termId = it.id
                requestState(State.IMPORT_MARKS_BY_ARGUMENTS)
            }
        }
    }

    private fun setLeftDrawable(textView: TextView, id: Int, color: Int) {
        textView.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context!!, id)!!.apply {
                    mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }, null, null, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        view.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 400 })


        val textColor = context.themeAttributeToColor(android.R.attr.textColorPrimary)
        view.findViewById<CardView>(R.id.cardImport)?.also { card ->
            setLeftDrawable(card.findViewById(R.id.textImport)!!, R.drawable.ic_import, textColor)

            card.foreground = context.themeAttributeToDrawable(android.R.attr.selectableItemBackground)

            card.setOnClickListener(::onMethodSelected)
        }

        view.findViewById<CardView>(R.id.cardMarkScales)?.also { card ->
            setLeftDrawable(card.findViewById(R.id.textMarkScales)!!, R.drawable.nav_marks, textColor)

            card.foreground = context.themeAttributeToDrawable(android.R.attr.selectableItemBackground)

            card.setOnClickListener(::onMethodSelected)
        }

        view.findViewById<CardView>(R.id.cardPoints)?.also { card ->
            setLeftDrawable(card.findViewById(R.id.textPoints)!!, R.drawable.ic_more, textColor)

            card.foreground = context.themeAttributeToDrawable(android.R.attr.selectableItemBackground)

            card.setOnClickListener(::onMethodSelected)
        }

        if (savedInstanceState == null) {
            val arguments = arguments!!
            when (arguments.getInt("method", NO_METHOD)) {
                METHOD_SELECTED_MARK_SCALE_GROUPS -> {
                    viewModel.apply {
                        //markScaleGroupId = arguments.getInt("markScaleGroupId", -1)
                        requestState(State.PREPARE_FOR_SCALE_MARKS)
                    }
                }
                METHOD_POINTS -> viewModel.requestState(State.PREPARE_FOR_POINTS_MARKS)
                METHOD_IMPORTED -> viewModel.requestState(State.IMPORT_MARKS_BY_ARGUMENTS)

                METHOD_IMPORT_FROM_SUBJECT -> {
                    viewModel.apply {
                        subjectId = arguments.getInt("subjectId", -1)
                        markScaleGroupId = arguments.getInt("markScaleGroupId", -1)
                        startTime = arguments.getLong("start")
                        endTime = arguments.getLong("end")
                        if (currentState != State.IMPORT_MARKS_BY_ARGUMENTS) {
                            requestState(State.CHOOSE_SUBJECT)
                        }
                    }
                }
            }
        }

        view.findViewById<View?>(R.id.goToMenu)?.setOnClickListener {
            MobiregPreferences.get(it.context).hasSavedAnyVirtualMark = false
            viewModel.marksList.clear()
            viewModel.requestState(State.NOTHING)
        }

        viewModel.currentState.also {
            it.removeObserver(stateObserver)
            it.observe(this, stateObserver)
            if (it.value == State.INITIALIZING || it.value == null)
                viewModel.requestState(State.NOTHING)
        }
    }

    private val stateObserver = Observer<State> {
        it ?: return@Observer
        when (it) {
            State.INITIALIZING -> viewModel.requestState(State.NOTHING)
            State.NOTHING -> {
                if (view?.findViewById<View?>(R.id.cardImport) == null) {
                    arguments?.putInt("method", NO_METHOD)
                    recreate()
                }
            }
            State.CHOOSE_SUBJECT -> onSubjectsToImportLoaded()
            State.CHOOSE_SCALE_GROUP -> onMarkScaleGroupsLoaded()
            State.CHOOSE_TERM -> onTermsLoaded()
            State.IMPORT_MARKS_BY_ARGUMENTS, State.IMPORTED -> {
                arguments?.putInt("method", METHOD_IMPORTED)
                if (view?.findViewById<View?>(R.id.cardImport) != null)
                    recreate()
                else {
                    onMarksToImportLoaded(it)
                    viewModel.requestState(State.IMPORTED)
                }

            }
            State.PREPARE_FOR_SCALE_MARKS -> {
                if (view?.findViewById<View?>(R.id.cardImport) != null) {
                    arguments?.putInt("method", METHOD_SELECTED_MARK_SCALE_GROUPS)
                    recreate()
                } else {
                    setAdapter(VirtualScaleMarksAdapter(this, viewModel.markScales))
                }
            }
            State.PREPARE_FOR_POINTS_MARKS -> {
                if (view?.findViewById<View?>(R.id.cardImport) != null) {
                    arguments?.putInt("method", METHOD_POINTS)
                    recreate()
                } else {
                    setAdapter(VirtualMarkPointsAdapter(this))
                }
            }
        }
    }

    private fun onMarksToImportLoaded(it: State) {
        val result = viewModel.marksToImport
        val shouldAddAgain = it != State.IMPORTED
        if (result.any { it.markValue != null }) {
            // mamy oceny punktowe
            setAdapter(VirtualMarkPointsAdapter(this).apply {
                if (shouldAddAgain)
                    result.forEach {
                        insertImported(it.markValue ?: 0f, it.weight)
                    }
            })
        } else {
            // mamy klasyczne oceny
            setAdapter(VirtualScaleMarksAdapter(this,
                    viewModel.markScales).apply {

                if (shouldAddAgain)
                    result.groupBy { it.parentId }.forEach { group ->
                        if (group.value.size == 1) {
                            // oceny bez poprawy
                            group.value.forEach {
                                insertImported(it.scaleId!!, it.weight)
                            }
                        } else {
                            val first = group.value.first()
                            val parent = insertImported(first.weight,
                                    first.parentType ?: MarkDao.PARENT_TYPE_COUNT_WORSE)

                            group.value.forEach {
                                insertImported(it.scaleId!!, parent)
                            }
                        }
                    }
            })
        }
    }


    @MainThread
    private fun setAdapter(adapter: VirtualBaseAdapter) {
        val mainList = view?.findViewById<RecyclerView?>(R.id.markParentsList) ?: return
        mainList.adapter = adapter
        view?.findViewById<View>(R.id.addNewMark)?.setOnClickListener {
            mainList.scrollToPosition(0)
            adapter.onNewMarkRequested()
        }
        val weakTitle = WeakReference(view?.findViewById<TextView?>(R.id.title))
        adapter.average.observe(this, Observer {
            weakTitle.get()?.apply {
                text = adapter.getNiceText(it)
            }
        })
        mainList.postDelayed({
            adapter.notifyMarkChanged()
        }, 100)
    }


    class VirtualMarkViewHolder(adapter: VirtualBaseAdapter, v: View) : RecyclerView.ViewHolder(v) {
        val btnIncrease = v.findViewById<ImageView>(R.id.btnIncrease)!!
        val btnDecrease = v.findViewById<ImageView>(R.id.btnDecrease)!!

        val btnIncrease2 = v.findViewById<ImageView?>(R.id.btnIncrease2)
        val btnDecrease2 = v.findViewById<ImageView?>(R.id.btnDecrease2)
        val textValue2 = v.findViewById<TextView?>(R.id.markValue2)

        val btnMore = v.findViewById<ImageView?>(R.id.btnMore)

        private val btnDelete = v.findViewById<ImageView>(R.id.btnDeleteMark)!!
        val btnAddNewMark = v.findViewById<ImageView?>(R.id.btnAddNewMark)
        val textParentType = v.findViewById<TextView?>(R.id.parentTypeText)
        val textValue = v.findViewById<TextView>(R.id.markValue)!!

        val title1 = v.findViewById<TextView?>(R.id.textWeight)
        val title2 = v.findViewById<TextView?>(R.id.title)

        init {
            btnDelete.setOnClickListener { adapter.deleteMarkAt(adapterPosition) }
            btnIncrease.setImageDrawable(adapter.increaseDrawable)
            btnDecrease.setImageDrawable(adapter.decreaseDrawable)
            btnIncrease2?.setImageDrawable(adapter.increaseDrawable)
            btnDecrease2?.setImageDrawable(adapter.decreaseDrawable)
            if (btnDelete.tag !is Int?)
                btnDelete.setImageDrawable(adapter.deleteAllDrawable)
            else
                btnDelete.setImageDrawable(adapter.deleteDrawable)
            btnAddNewMark?.setImageDrawable(adapter.addMarkDrawable)
            btnMore?.setImageDrawable(adapter.expandDrawable)
        }
    }


    abstract class VirtualBaseAdapter(f: VirtualMarksFragment
    ) : RecyclerView.Adapter<VirtualMarkViewHolder>() {
        companion object {
            const val TYPE_SCALE_PARENT = 0
            const val TYPE_SCALE_CHILD = 1
            const val TYPE_SCALE_SINGLE = 2

            const val TYPE_POINTS_SINGLE = 3
        }

        val context = f.context!!

        private val drawablesColor = context.themeAttributeToColor(android.R.attr.textColorPrimary)
        private fun getDrawable(id: Int): Drawable {
            return AppCompatResources.getDrawable(context, id)!!.tintSelf(drawablesColor)
        }

        val increaseDrawable = getDrawable(R.drawable.ic_navigate_next)
        val decreaseDrawable = getDrawable(R.drawable.ic_navigate_before)
        val deleteDrawable = getDrawable(R.drawable.ic_delete)
        val deleteAllDrawable = getDrawable(R.drawable.ic_delete_sweep)
        val addMarkDrawable = getDrawable(R.drawable.ic_add_box)
        val expandDrawable = getDrawable(R.drawable.ic_more)

        private val mAverageData = MutableLiveData<String?>().apply { value = null }
        val average = mAverageData.asImmutable

        fun notifyMarkChanged() {
            Handler(Looper.getMainLooper()).post(updateAverageRunnable)
        }

        private val updateAverageRunnable = Runnable {
            mAverageData.value = calculateAverage()
        }

        protected abstract fun calculateAverage(): String?

        protected val mainList = f.viewModel.marksList
        protected val inflater = LayoutInflater.from(context)!!

        init {
            mainList.forEach { it.adapter = WeakReference(this) }
        }

        override fun getItemCount() = mainList.size

        override fun onBindViewHolder(holder: VirtualMarkViewHolder, pos: Int) {
            val item = mainList[pos]
            item.bindToView(holder)
        }

        override fun getItemViewType(position: Int): Int {
            return mainList[position].type
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): VirtualMarkViewHolder {
            return VirtualMarkViewHolder(this, inflater.inflate(when (type) {
                TYPE_SCALE_SINGLE -> R.layout.mark_calculator_single_list_item
                TYPE_SCALE_CHILD -> R.layout.mark_calculator_child_list_item
                TYPE_SCALE_PARENT -> R.layout.mark_calculator_parent_list_item
                TYPE_POINTS_SINGLE -> R.layout.mark_calculator_single_list_item
                else -> throw IllegalStateException()
            }, parent, false))
        }

        protected fun insertMark(mark: VirtualMarkListItem, pos: Int) {
            mark.adapter = WeakReference(this)
            mainList.add(pos, mark)
            notifyItemInserted(pos)
        }

        fun deleteMarkAt(pos_: Int) {
            var pos = pos_
            notifyMarkChanged()
            val iterator = mainList.iterator()
            var previous: VirtualMarkListItem? = null
            for (i in 0 until pos)
                previous = iterator.next()


            val item = iterator.next()
            iterator.remove()
            var removed = 1

            when (item) {
                is VirtualMarkParent -> while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it !is VirtualMarkChild || it.parent != item)
                        break
                    iterator.remove()
                    removed++
                }
                is VirtualMarkChild -> if (previous is VirtualMarkParent
                        && (!iterator.hasNext() || iterator.next().type != TYPE_SCALE_CHILD)) {
                    mainList.removeAt(--pos)
                    removed += 1
                }
            }


            notifyItemRangeRemoved(pos, removed)
        }

        abstract fun getNiceText(value: String?): CharSequence

        abstract fun onNewMarkRequested()
    }

    class VirtualScaleMarksAdapter(
            f: VirtualMarksFragment,
            val markScales: List<MarkDao.MarkScaleShortInfo>)
        : VirtualBaseAdapter(f) {

        val markScalesTitles = Array(markScales.size) { markScales[it].abbreviation }
        var lastWeight = 1f
        var lastMarkScaleIndex = markScalesTitles.size.div(2)

        private fun insertImported(markScaleId: Int,
                                   weight: Float,
                                   parent: VirtualMarkParent?) {
            if (parent != null) {
                val pos = mainList.indexOf(parent) + 1
                insertMark(VirtualMarkChild(parent)
                        .apply { markScaleIndex = markScales.indexOfFirst { it.id == markScaleId } }, pos)
            } else {
                insertMark(VirtualMarkSingle().apply {
                    markScaleIndex = markScales.indexOfFirst { it.id == markScaleId }
                    this.weight = weight
                }, if (mainList.isEmpty()) 0 else mainList.size - 1)
            }
        }

        fun insertImported(markScaleId: Int, parent: VirtualMarkParent) = insertImported(markScaleId, 0f, parent)

        fun insertImported(markScaleId: Int,
                           weight: Float) = insertImported(markScaleId, weight, null)

        fun insertImported(weight: Float,
                           parentType: Int): VirtualMarkParent {
            val mark = VirtualMarkParent().apply {
                this.parentType = parentType
                this.weight = weight
            }
            insertMark(mark, if (mainList.isEmpty()) 0 else mainList.size - 1)
            return mark
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

        override fun calculateAverage(): String? {
            return calculateAverageScaleMarks().let {
                if (it.isNaN()) null else "%.2f".format(it)
            }
        }

        override fun onNewMarkRequested() {
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

        override fun getNiceText(value: String?) = if (value == null) "Tu pokaże się twoja średnia"
        else "Średnia ważona wynosi $value"
    }

    class VirtualMarkPointsAdapter(f: VirtualMarksFragment) : VirtualBaseAdapter(f) {
        override fun onNewMarkRequested() {
            insertMark(VirtualPointsMark(), 0)
        }

        fun insertImported(value: Float,
                           maxValue: Float) {
            insertMark(VirtualPointsMark().also {
                it.gotPointsSum = value
                it.weight = maxValue
            }, if (mainList.isEmpty()) 0 else mainList.size - 1)
        }

        override fun calculateAverage(): String? {
            var totalSum = 0f
            var totalBase = 0f

            mainList.filterIsInstance<VirtualPointsMark>()
                    .forEach {
                        totalSum += it.gotPointsSum
                        totalBase += it.basePointsSum
                    }

            if (totalBase == 0f)
                return null
            return "%.1f na %.1f czyli %.2f%%".format(totalSum, totalBase, totalSum / totalBase * 100f)
        }

        override fun getNiceText(value: String?) = if (value != null) "Twój wynik punktowy: $value"
        else "Tu pokaże się ilość zdobytych punktów"
    }
}
