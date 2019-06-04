import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.fragment.AboutVirtualMarksFragment
import jakubweg.mobishit.fragment.MarkDetailsFragment
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_POINTS_SINGLE
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_SCALE_CHILD
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_SCALE_PARENT
import jakubweg.mobishit.helper.VirtualMarkBase.Companion.TYPE_SCALE_SINGLE
import java.lang.ref.WeakReference

class VirtualMarksFragment : Fragment() {
    companion object {
        fun newInstance() = VirtualMarksFragment()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_virtual_marks, container, false)
    }

    private val viewModel get() = ViewModelProviders.of(this)[VirtualMarksModel::class.java]

    private val listView get() = this.view?.findViewById<RecyclerView?>(R.id.markParentsList)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<View?>(R.id.addNewMark)?.setOnClickListener {
            (listView?.adapter as? VirtualMarksAdapter?)?.onAddRequested()
            listView?.scrollToPosition(0)
        }

        view.findViewById<View?>(R.id.clearAllMarks)?.setOnClickListener {
            context!!.also { context ->
                MobiregPreferences.get(context).markHavingNoSavedMarks()
                (context as? MainActivity?)?.onBackPressed()

            }
        }

        if (viewModel.marks.value == null)
            viewModel.requestLoad()

        viewModel.marks.observe(this, Observer {
            listView?.adapter = VirtualMarksAdapter(this, context!!,
                    it?.toMutableList() ?: return@Observer,
                    viewModel.markScales)
        })
    }

    private fun onAverageCalculated(average: Spanned) {
        view?.textView(R.id.title)?.text = average
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.marks.removeObservers(this)
        viewModel.marks.value = (listView?.adapter as? VirtualMarksAdapter)?.marksList ?: return
    }

    class VirtualMarksAdapter(private val fragment: VirtualMarksFragment,
                              val context: Context,
                              val marksList: MutableList<VirtualMarkBase>,
                              val markScales: List<MarkDao.MarkScaleShortInfo>)
        : RecyclerView.Adapter<VirtualMarkViewHolder>() {
        val markScalesFiltered = markScales.filter { it.selectable }.map { it.abbreviation }.toTypedArray()

        var lastScaleIndex = markScales.size / 2
        var lastWeight = 1f

        private val recalculationRunnable = Runnable {
            val iterator = marksList.peekableIterator()
            var gotPointsSum = 0f
            var basePointsSum = 0f
            var scaleValueSum = 0f
            var weightSum = 0f

            while (iterator.hasNext()) {
                val it = iterator.next()
                when (it.type) {
                    TYPE_POINTS_SINGLE -> {
                        (it as VirtualMarkPoints).also {
                            gotPointsSum += it.pointsValue
                            basePointsSum += it.baseValue
                        }
                    }
                    TYPE_SCALE_SINGLE -> {
                        (it as VirtualMarkScaleSingle).also {
                            scaleValueSum += markScales[it.scaleIndex].markValue * it.weight
                            weightSum += it.weight
                        }
                    }
                    TYPE_SCALE_PARENT -> {
                        (it as VirtualMarkParent).also { parent ->
                            when (parent.parentType) {
                                MarkDao.PARENT_TYPE_COUNT_EVERY -> {
                                    while (iterator.hasNext()) {
                                        if (iterator.peek().type != TYPE_SCALE_CHILD)
                                            break
                                        (iterator.next() as VirtualMarkChild).also {
                                            scaleValueSum += markScales[it.scaleIndex].markValue * parent.weight
                                            weightSum += parent.weight
                                        }
                                    }
                                }

                                MarkDao.PARENT_TYPE_COUNT_AVERAGE -> {
                                    var scaleValueSum1 = 0f
                                    var count = 0f
                                    while (iterator.hasNext()) {
                                        if (iterator.peek().type != TYPE_SCALE_CHILD)
                                            break
                                        (iterator.next() as VirtualMarkChild).also {
                                            scaleValueSum1 += markScales[it.scaleIndex].markValue * parent.weight
                                            count++
                                        }
                                    }
                                    if (count > 0) {
                                        scaleValueSum += scaleValueSum1 / count
                                        weightSum += parent.weight
                                    }
                                }


                                MarkDao.PARENT_TYPE_COUNT_BEST -> {
                                    val values = mutableListOf<Float>()
                                    while (iterator.hasNext()) {
                                        if (iterator.peek().type != TYPE_SCALE_CHILD)
                                            break
                                        (iterator.next() as VirtualMarkChild).also {
                                            values += markScales[it.scaleIndex].markValue
                                        }
                                    }
                                    if (values.isNotEmpty()) {
                                        scaleValueSum += values.max()!! * parent.weight
                                        weightSum += parent.weight
                                    }
                                }

                                MarkDao.PARENT_TYPE_COUNT_WORSE -> {
                                    val values = mutableListOf<Float>()
                                    while (iterator.hasNext()) {
                                        if (iterator.peek().type != TYPE_SCALE_CHILD)
                                            break
                                        (iterator.next() as VirtualMarkChild).also {
                                            values += markScales[it.scaleIndex].markValue
                                        }
                                    }
                                    if (values.isNotEmpty()) {
                                        scaleValueSum += values.min()!! * parent.weight
                                        weightSum += parent.weight
                                    }
                                }

                                MarkDao.PARENT_TYPE_COUNT_LAST -> {
                                    var value = Float.NaN
                                    while (iterator.hasNext()) {
                                        if (iterator.peek().type != TYPE_SCALE_CHILD)
                                            break
                                        (iterator.next() as VirtualMarkChild).also {
                                            value = markScales[it.scaleIndex].markValue
                                        }
                                    }
                                    if (!value.isNaN()) {
                                        scaleValueSum += value * parent.weight
                                        weightSum += parent.weight
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val text = CommonFormatter.formatMarksAverage(gotPointsSum, basePointsSum, scaleValueSum / weightSum, context.resources, null, false)
            fragment.onAverageCalculated(text)
        }

        init {
            marksList.forEach { it.adapter = WeakReference(this) }
            requestAverageRecalculation()
        }

        private val addNewType =
                if (MobiregPreferences.get(context).savedVirtualMarksState == AboutVirtualMarksFragment.STATE_HAVING_SCALE_MARKS)
                    TYPE_SCALE_SINGLE else TYPE_POINTS_SINGLE

        fun onAddChildRequested(position: Int) {
            val item = VirtualMarkChild(null, lastScaleIndex)
            item.adapter = WeakReference(this)
            marksList.add(position + 1, item)
            notifyItemInserted(position + 1)
            requestAverageRecalculation()
        }

        fun onAddRequested() {
            val item = when (addNewType) {
                TYPE_POINTS_SINGLE -> VirtualMarkPoints(null, 10f, 10f)
                TYPE_SCALE_SINGLE -> VirtualMarkScaleSingle(null, lastScaleIndex, lastWeight)
                else -> null
            } ?: return

            item.adapter = WeakReference(this)
            marksList.add(0, item)
            notifyItemInserted(0)
            requestAverageRecalculation()
        }

        fun onDeleteRequested(position: Int) {
            val item = marksList[position]

            var removedCount = 1
            if (item.type == TYPE_SCALE_PARENT) {
                val iterator = marksList.iterator()
                for (i in 0..position) iterator.next()
                iterator.remove()
                while (iterator.hasNext()) {
                    if (iterator.next().type == TYPE_SCALE_CHILD) {
                        iterator.remove()
                        removedCount++
                    } else break
                }
            } else {
                marksList.removeAt(position)
            }

            notifyItemRangeRemoved(position, removedCount)
            requestAverageRecalculation()
        }

        fun onExpandScaleMarkRequested(position: Int) {
            if(position == -1) // I just had crash caused by this, so..
                return
            val item = marksList[position] as VirtualMarkScaleSingle
            marksList.removeAt(position)
            marksList.add(position, VirtualMarkParent(MarkDao.PARENT_TYPE_COUNT_AVERAGE, item.weight, item.weight)
                    .also { it.adapter = WeakReference(this) })
            notifyItemChanged(position)
            marksList.add(position + 1, VirtualMarkChild(item.originalMarkId, item.scaleIndex)
                    .also { it.adapter = WeakReference(this) })
            notifyItemInserted(position + 1)
        }

        fun requestAverageRecalculation() {
            Handler(Looper.getMainLooper()).postDelayed(recalculationRunnable, 50L)
        }

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

        private val inflater = LayoutInflater.from(context)!!

        override fun getItemCount() = marksList.size

        override fun getItemViewType(position: Int) = marksList[position].type

        override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): VirtualMarkViewHolder {
             return VirtualMarkViewHolder(context as DoublePanelActivity, this, inflater.inflate(
                    when (type) {
                        TYPE_POINTS_SINGLE, TYPE_SCALE_SINGLE -> R.layout.mark_calculator_single_list_item
                        TYPE_SCALE_PARENT -> R.layout.mark_calculator_parent_list_item
                        TYPE_SCALE_CHILD -> R.layout.mark_calculator_child_list_item
                        else -> throw IllegalArgumentException()
                    }, viewGroup, false), type)
        }

        override fun onBindViewHolder(holder: VirtualMarkViewHolder, position: Int) {
            val item = marksList[position]
            item.currentViewHolder = WeakReference(holder)
            holder.setListener(item)
            holder.stateIndicator?.virtualMark = item
            holder.stateIndicator?.parentVirtualMark =
                if(item.type == TYPE_SCALE_CHILD)
                    marksList.take(position).filterIsInstance<VirtualMarkParent>().lastOrNull()
                else null
            item.applyToViews()
        }
    }

    abstract class VirtualMarksChangeListener {
        /**
         * @param valueChange it is positive when clicked plus, negative when minus,
         * and zero when value clicked
         * @return true if click was consumed (changed or caused anything), false if click was ignored
         */
        open fun onFirstRowClicked(valueChange: Byte): Boolean = false

        /**
         * @param valueChange it is positive when clicked plus, negative when minus,
         * and zero when value clicked
         */
        open fun onSecondRowClicked(valueChange: Byte) = Unit

        open fun onDeleteClicked() = Unit

        open fun onExpandClicked() = Unit

        open fun onAddMarkClicked() = Unit

        open fun onParentTypeTextClicked() = Unit
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class VirtualMarkViewHolder(parentActivity: DoublePanelActivity, parent: VirtualMarksAdapter, v: View, type: Int)
        : RecyclerView.ViewHolder(v) {

        val plusBtn1 = v.findViewById<ImageView?>(R.id.btnIncrease)!!
        val minusBtn1 = v.findViewById<ImageView?>(R.id.btnDecrease)!!
        val markValue1 = v.textView(R.id.markValue)!!

        val plusBtn2 = v.findViewById<ImageView?>(R.id.btnIncrease2)
        val minusBtn2 = v.findViewById<ImageView?>(R.id.btnDecrease2)
        val markValue2 = v.textView(R.id.markValue2)

        val addMarkBtn = v.findViewById<ImageView?>(R.id.btnAddNewMark)
        val deleteBtn = v.findViewById<ImageView?>(R.id.btnDeleteMark)!!
        val expandBtn = v.findViewById<ImageView?>(R.id.btnMore)
        val parentTypeText = v.findViewById<TextView?>(R.id.parentTypeText)

        val stateIndicator: VirtualMarkStateView?

        init {
            val markStateView = v.findViewById<View>(R.id.markState)
            if(markStateView != null)
                stateIndicator = VirtualMarkStateView(markStateView, parentActivity)
            else
                stateIndicator = null
        }

        private var currentListener = WeakReference<VirtualMarksChangeListener?>(null)
        fun setListener(l: VirtualMarksChangeListener?) {
            currentListener = WeakReference(l)
        }

        private inline fun View?.onClick(crossinline function: (VirtualMarksChangeListener) -> Unit) {
            if (this == null) return
            setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION)
                    function(currentListener.get() ?: return@setOnClickListener)
            }
        }

        init {
            addMarkBtn.onClick { it.onAddMarkClicked() }
            expandBtn.onClick { it.onExpandClicked() }
            deleteBtn.onClick { it.onDeleteClicked() }
            parentTypeText.onClick { it.onParentTypeTextClicked() }

            plusBtn1.onClick { it.onFirstRowClicked(+1) }
            markValue1.onClick { it.onFirstRowClicked(0) }
            minusBtn1.onClick { it.onFirstRowClicked(-1) }

            plusBtn2.onClick { it.onSecondRowClicked(+1) }
            markValue2.onClick { it.onSecondRowClicked(0) }
            minusBtn2.onClick { it.onSecondRowClicked(-1) }

            plusBtn1.setImageDrawable(parent.increaseDrawable)
            minusBtn1.setImageDrawable(parent.decreaseDrawable)
            plusBtn2?.setImageDrawable(parent.increaseDrawable)
            minusBtn2?.setImageDrawable(parent.decreaseDrawable)
            deleteBtn.setImageDrawable(if (deleteBtn.tag !is Int?) parent.deleteAllDrawable
                else parent.deleteDrawable)
            addMarkBtn?.setImageDrawable(parent.addMarkDrawable)
            expandBtn?.setImageDrawable(parent.expandDrawable)

            if (type == TYPE_POINTS_SINGLE) {
                v.textView(R.id.textWeight)?.precomputedText = "Zdobyte:"
                v.textView(R.id.title)?.precomputedText = "Baza:"
            }
        }
    }

    class VirtualMarkStateView(view: View, private val parentActivity: DoublePanelActivity) {
        private val stateText = view.textView(R.id.stateText)!!
        private val peekMarkText = view.textView(R.id.peekMarkText)!!
        private val markDescriptionLayout = view.findViewById<View>(R.id.orgiginalMarkDescription)!!
        private val markValue = view.textView(R.id.markValue)!!
        private val markTitle = view.textView(R.id.markTitle)!!
        private val markDescription = view.textView(R.id.markDescription)!!
        private val canShowOrgMarkDesc: Boolean

        var virtualMark: VirtualMarkBase? = null
            set(v) {
                field = v
                updateNewVirtualMark()
            }

        var parentVirtualMark: VirtualMarkParent? = null
            set(v){
                field = v
                updateNewVirtualMark()
            }

        init {
            view.setOnClickListener {
                virtualMark?.also { virtualMark ->
                    if(virtualMark.originalMarkId != null)
                        MarkDetailsFragment.newInstance(virtualMark.originalMarkId).showSelf(parentActivity)
                }
            }

            val dispSize = Point()
            parentActivity.windowManager.defaultDisplay.getSize(dispSize)
            canShowOrgMarkDesc = dispSize.x >= dispSize.y // more reliable way to check if landscape

            updateNewVirtualMark()
        }

        private fun updateNewVirtualMark(){
            //TODO: adjust mark value circle texture if has a sibling
            markDescriptionLayout.visibility =
                if(canShowOrgMarkDesc && virtualMark?.originalMarkId != null) View.VISIBLE
                else View.GONE
            virtualMark?.also { virtualMark ->
                if (virtualMark.originalMarkId != null && canShowOrgMarkDesc) {
                    val markDao = AppDatabase.getAppDatabase(parentActivity).markDao
                    val markInfo = markDao.getMarkOverviewInfo(virtualMark.originalMarkId) ?: return
                    markTitle.text = markInfo.description.takeUnless { it.isBlank() } ?: "Bez tytułu"
                    markValue.precomputedText = CommonFormatter.formatMarkValueBig(
                            markInfo.abbreviation, markInfo.markScaleValue, markInfo.markPointsValue)
                    markDescription.also {
                        val title = CommonFormatter.formatMarkShortDescription(markInfo.markValueMax, markInfo.markPointsValue,
                                markInfo.countPointsWithoutBase, markInfo.noCountToAverage, markInfo.weight)
                        if (title == null)
                            it.visibility = View.GONE
                        else {
                            it.precomputedText = title
                            it.visibility = View.VISIBLE
                        }
                    }
                }

                updateVirtualMarkValue()
            }
        }

        fun updateVirtualMarkValue(){
            virtualMark?.also { virtualMark ->
                stateText.text = if(virtualMark.originalMarkId == null)
                    "Nowa"
                else {
                    val prefs = MobiregPreferences.get(parentActivity)
                    val markDao = AppDatabase.getAppDatabase(parentActivity).markDao
                    var markScales: List<MarkDao.MarkScaleShortInfo>? = null

                    val orgMark = markDao.getMarkValueInfo(virtualMark.originalMarkId)
                    if(orgMark == null)
                        "Błąd - nie znaleziono oceny"
                    else {
                        if(virtualMark.type == TYPE_SCALE_PARENT)
                            throw IllegalStateException()
                        var isCurrent = false
                        if (virtualMark is VirtualMarkPoints && virtualMark.pointsValue == orgMark.markValue && virtualMark.baseValue == orgMark.weight)
                            isCurrent = true
                        else {
                            markScales = markScales ?: markDao.getMarkScalesByGroupId(prefs.savedMarkScaleGroupId)
                            val orgScaleIndex = markScales.indexOfFirst { it.id == orgMark.scaleId }
                            if (orgScaleIndex != -1){
                                if(virtualMark is VirtualMarkScaleSingle && virtualMark.scaleIndex == orgScaleIndex && virtualMark.weight == orgMark.weight)
                                    isCurrent = true
                                else if(virtualMark is VirtualMarkChild && virtualMark.scaleIndex == orgScaleIndex) {
                                    isCurrent = parentVirtualMark.let { parentVirtualMark ->
                                        if(parentVirtualMark == null)
                                            return@let true
                                        return@let parentVirtualMark.weight == parentVirtualMark.originalWeight
                                    }
                                }
                            }
                        }

                        if(isCurrent)
                            "Obecna"
                        else
                            "Zmieniona"
                    }
                }

                peekMarkText.visibility = if(virtualMark.originalMarkId == null) View.GONE else View.VISIBLE
            }
        }
    }
}