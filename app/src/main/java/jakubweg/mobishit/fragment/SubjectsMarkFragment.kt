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
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.helper.textView
import jakubweg.mobishit.model.SubjectsMarkModel


class SubjectsMarkFragment : Fragment(), MarksViewOptionsFragment.OptionsChangedListener {
    companion object {

        fun newInstance(subjectId: Int, subjectName: String, viewTransitionName: String?) = SubjectsMarkFragment().apply {
            arguments = Bundle().also {
                it.putInt("subjectId", subjectId)
                it.putString("subjectName", subjectName)
                it.putString("transitionName", viewTransitionName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        (activity as? MainActivity?)?.addOptionListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
            sharedElementReturnTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_subject_marks, container, false)

    private val viewModel by lazy { ViewModelProviders.of(this)[SubjectsMarkModel::class.java] }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val (subjectId, subjectName, transitionName) =
                arguments!!.run {
                    Triple(getInt("subjectId"), getString("subjectName")!!, getString("transitionName")
                            ?: null)
                }

        viewModel.init(subjectId)

        val subjectNameText = view.textView(R.id.subject_name)!!
        subjectNameText.text = subjectName
        ViewCompat.setTransitionName(subjectNameText, transitionName)

        viewModel.marks.observe(this, Observer {
            it ?: return@Observer
            onTermChanged()
        })
    }

    override fun onTermChanged() {
        MobiregPreferences.get(context ?: return).apply {
            val termId = lastSelectedTerm
            setUpMarksAdapter(viewModel.marks.value?.get(termId) ?: return)
            setUpAveragesInfo(viewModel.averages.value?.get(termId) ?: return)
        }
    }


    override fun onOtherOptionsChanged() {
        viewModel.requestMarksAgain()
    }

    private fun setUpMarksAdapter(marks: List<MarkDao.MarkShortInfo>) {
        view?.findViewById<RecyclerView>(R.id.marksList)?.apply {
            startAnimation(AlphaAnimation(0f, 1f).also {
                it.duration = 400L
            })
            adapter = MarkAdapter(context, marks) { item ->
                MarkDetailsFragment.newInstance(item.id).showSelf(activity as? MainActivity)
            }
        }
    }

    private fun setUpAveragesInfo(average: AverageCacheData) {
        view?.textView(R.id.averageInfoText)?.text = average.averageText
    }

    class MarkAdapter(context: Context,
                      private val list: List<MarkDao.MarkShortInfo>,
                      private var onMarkClickListener: ((MarkDao.MarkShortInfo) -> Unit)? = null)
        : RecyclerView.Adapter<MarkAdapter.ViewHolder>() {
        private val inflater = LayoutInflater.from(context)!!

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

        private inline val Float.prettyMe: String
            get() = String.format("%.1f", this)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also { info ->
                holder.markTitle.text = (info.description.takeUnless { it.isBlank() }
                        ?: "Bez tytułu")
                ViewCompat.setTransitionName(holder.markTitle, "ma$position")
                holder.markValue.precomputedText = when {
                    info.abbreviation != null -> info.abbreviation
                    //info.markPointsValue >= 0 && info.markValueMax > 0 ->
                    //    "${info.markPointsValue}\n${info.markValueMax}"
                    info.markPointsValue >= 0 -> info.markPointsValue.toString() trimEndToLength 4
                    else -> "Wut?"
                }
                holder.markDescription?.precomputedText = when {
                    info.abbreviation == null && info.markValueMax > 0 ->
                        StringBuilder()
                                .append(info.markPointsValue.div(info.markValueMax).times(100f).prettyMe)
                                .append("% • maksymalnie ")
                                .append(info.markValueMax.prettyMe)
                                .apply {
                                    if (info.countPointsWithoutBase == true)
                                        append(" \u2022 Poza bazą")
                                }
                                .toString()
                    info.weight > 0 -> "Waga ${info.weight.prettyMe}"
                    else -> ""
                }
            }
        }

        override fun getItemViewType(position: Int) = list[position].viewType

        override fun getItemCount() = list.size

        private fun onItemClicked(position: Int) {
            onMarkClickListener?.invoke(list[position])
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

