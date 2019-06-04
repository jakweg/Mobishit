package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.annotation.IntRange
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MarkOptionsListener
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.fragment.SubjectsMarkFragment.MarkAdapter.Companion.TYPE_SINGLE
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.SubjectsMarkModel
import java.lang.ref.WeakReference


class SubjectsMarkFragment : Fragment(),
        MarksViewOptionsFragment.OptionsChangedListener {
    companion object {

        fun newInstance(subjectId: Int, subjectName: String, viewTransitionName: String?) = SubjectsMarkFragment().apply {
            arguments = Bundle().also {
                it.putInt("subjectId", subjectId)
                it.putString("subjectName", subjectName)
                it.putString("transitionName", viewTransitionName)
            }
        }
    }

    private lateinit var listener: MarkOptionsListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        listener = MarkOptionsListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
            sharedElementReturnTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_subject_marks, container, false)

    private val viewModel by lazy { ViewModelProviders.of(this)[SubjectsMarkModel::class.java] }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments!!.apply {
            val subjectId = getInt("subjectId")
            val subjectName = getString("subjectName")!!
            val transitionName = getString("transitionName")


            viewModel.init(subjectId)

            val subjectNameText = view.textView(R.id.subject_name)!!
            subjectNameText.text = subjectName
            ViewCompat.setTransitionName(subjectNameText, transitionName)
        }

        viewModel.marks.observe(this, Observer {
            it ?: return@Observer
            onOptionsChanged(true,
                    false,
                    false)
        })
    }


    override fun onOptionsChanged(changedTerm: Boolean,
                                  changedOrder: Boolean,
                                  changedGrouping: Boolean) {
        if (changedGrouping || changedOrder)
            viewModel.shouldReorderEverything = true

        val prefs = MobiregPreferences.get(context!!)
        val termId = prefs.lastSelectedTerm

        val averageData = viewModel.averages.value?.get(termId) ?: return
        val marks = viewModel.marks.value?.get(termId) ?: return

        setUpAveragesInfo(averageData)
        setMarksAdapter(if (viewModel.shouldReorderEverything) {
            val isGroupingEnabled = prefs.groupMarksByParent
            if (!isGroupingEnabled)
                marks.forEach { it.viewType = TYPE_SINGLE }
            AverageCalculator.sortMarks(marks, prefs.markSortingOrder, isGroupingEnabled)
        } else marks)
    }

    private fun setMarksAdapter(marks: List<MarkDao.MarkShortInfo>) {
        view?.findViewById<RecyclerView>(R.id.marksList)?.apply {
            adapter.also {
                if (it is MarkAdapter)
                    it.setMarks(marks)
                else {
                    startAnimation(AlphaAnimation(0f, 1f).apply {
                        duration = 400L
                    })
                    adapter = MarkAdapter(context!!, this@SubjectsMarkFragment).apply {
                        setHasStableIds(true)
                        setMarks(marks)
                    }
                }
            }
        }
    }

    private fun setUpAveragesInfo(average: AverageCacheData) {
        view?.textView(R.id.averageInfoText)?.text =
            CommonFormatter.formatMarksAverage(average.gotPointsSum, average.baseSum, average.weightedAverage, resources, null, false)
    }

    class MarkAdapter(context: Context,
                      f: SubjectsMarkFragment?)
        : RecyclerView.Adapter<MarkAdapter.ViewHolder>() {
        private val weakFragment = WeakReference(f)
        private val inflater = LayoutInflater.from(context)!!

        private var list = emptyList<MarkDao.MarkShortInfo>()

        fun setMarks(list: List<MarkDao.MarkShortInfo>) {
            this.list = list
            notifyDataSetChanged()
        }

        companion object {
            const val TYPE_SINGLE = 0
            const val TYPE_PARENT_FIRST = 1
            const val TYPE_PARENT_MIDDLE = 2
            const val TYPE_PARENT_LAST = 3
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(when (viewType) {
                TYPE_SINGLE -> R.layout.mark_single_list_item
                TYPE_PARENT_FIRST -> R.layout.mark_parent_first_list_item
                TYPE_PARENT_LAST -> R.layout.mark_parent_last_list_item
                TYPE_PARENT_MIDDLE -> R.layout.mark_parent_middle_list_item
                else -> throw IllegalStateException()
            }, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also { info ->
                ViewCompat.setTransitionName(holder.markTitle, "ma$position")
                holder.markTitle.text = (info.description.takeUnless { it.isBlank() }
                        ?: "Bez tytułu")
                holder.markValue.precomputedText = CommonFormatter.formatMarkValueBig(
                        info.abbreviation, info.markScaleValue, info.markPointsValue)
                holder.markDescription?.also {
                    val title = CommonFormatter.formatMarkShortDescription(info.markValueMax, info.markPointsValue,
                            info.countPointsWithoutBase, info.noCountToAverage, info.weight)
                    if (title == null) {
                        it.visibility = View.GONE
                    } else {
                        it.precomputedText = title
                        it.visibility = View.VISIBLE
                    }
                }
            }
        }

        override fun getItemViewType(position: Int) = list[position].viewType

        override fun getItemCount() = list.size

        override fun getItemId(position: Int): Long {
            return list[position].id.toLong()
        }

        private fun onItemClicked(position: Int) {
            weakFragment.get()?.apply {
                MarkDetailsFragment
                        .newInstance(list[position].id)
                        .showSelf(activity)
            }
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val markTitle = v.textView(R.id.markTitle)!!
            val markValue = v.textView(R.id.markValue)!!
            val markDescription = v.textView(R.id.markDescription)

            init {
                v.setOnClickListener { onItemClicked(adapterPosition) }
            }
        }

        private infix fun String.trimEndToLength(@IntRange(from = 0L) maxLength: Int) = if (length <= maxLength) this else substring(0, maxLength + 1)
    }


}

