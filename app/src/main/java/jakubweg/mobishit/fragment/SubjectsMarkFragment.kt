package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.database.DataSetObserver
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
import android.widget.*
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.AverageCalculator
import jakubweg.mobishit.model.SubjectsMarkModel

class SubjectsMarkFragment : Fragment() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
            sharedElementReturnTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.subject_marks_fragment, container, false)

    private val viewModel by lazy { ViewModelProviders.of(this)[SubjectsMarkModel::class.java] }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val (subjectId, subjectName, transitionName) =
                arguments!!.run {
                    Triple(getInt("subjectId"), getString("subjectName")!!, getString("transitionName")
                            ?: null)
                }

        viewModel.init(subjectId)


        val subjectNameText = view.findViewById<TextView>(R.id.subject_name)!!
        subjectNameText.text = subjectName
        ViewCompat.setTransitionName(subjectNameText, transitionName)

        viewModel.terms.observe(this, Observer {
            it ?: return@Observer
            setUpTermsSpinner(it)
        })
    }

    private fun setUpTermsSpinner(terms: List<MarkDao.TermShortInfo>) {
        view!!.findViewById<Spinner>(R.id.termsSpinner).also { spinner ->
            spinner.adapter = TermSpinnerAdapter(context!!, terms)

            val listener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                var ignoreNext = false

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (ignoreNext) {
                        ignoreNext = false
                        return
                    }
                    if (viewModel.selectedTermId == id.toInt())
                        return
                    viewModel.selectedTermId = id.toInt()
                    onTermChanged()
                }
            }

            if (viewModel.selectedTermId != 0) {
                listener.ignoreNext = true
                spinner.setSelection(terms.indexOfFirst { it.id == viewModel.selectedTermId })
                onTermChanged()
            }

            spinner.onItemSelectedListener = listener
        }
    }

    private fun setUpMarksAdapter(marks: List<MarkDao.MarkShortInfo>) {
        view?.findViewById<RecyclerView>(R.id.marksList)?.apply {
            adapter = MarkAdapter(context, marks) { item, view ->
                val subjectName = this@SubjectsMarkFragment
                        .view?.findViewById<TextView>(R.id.subject_name) ?: return@MarkAdapter

                (activity as? DoublePanelActivity?)?.applyNewDetailsFragment(
                        view, subjectName, MarkDetailsFragment.newInstance(item.id,
                        item.description, ViewCompat.getTransitionName(view),
                        subjectName.text.toString(), ViewCompat.getTransitionName(subjectName)))
            }
        }
    }

    private fun setUpAveragesInfo(average: AverageCalculator.AverageCalculationResult) {
        view!!.findViewById<TextView>(R.id.averageInfoText).text = average.averageText
    }

    private fun onTermChanged() {
        setUpMarksAdapter(viewModel.marks.value?.get(viewModel.selectedTermId) ?: return)
        setUpAveragesInfo(viewModel.averages.value?.get(viewModel.selectedTermId) ?: return)
    }

    private class TermSpinnerAdapter(private val context: Context, private val terms: List<MarkDao.TermShortInfo>) : SpinnerAdapter {

        override fun isEmpty() = terms.isEmpty()
        override fun getCount() = terms.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: TextView(context).apply {
                layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
                val termsItemPadding = context.resources.getDimensionPixelSize(R.dimen.terms_item_padding)
                setPadding(termsItemPadding, termsItemPadding, termsItemPadding, termsItemPadding)
                textSize = 16f //sp
            }
            (view as TextView).text = terms[position].name
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?) = getView(position, convertView, parent)


        override fun getItemViewType(position: Int) = 0
        override fun getViewTypeCount() = 1
        override fun hasStableIds() = true


        override fun getItem(position: Int) = null
        override fun getItemId(position: Int) = terms[position].id.toLong()

        /*private val observers = mutableListOf<DataSetObserver?>()
        override fun registerDataSetObserver(observer: DataSetObserver?) { observers.add(observer); }
        override fun unregisterDataSetObserver(observer: DataSetObserver?) { observers.remove(observer) }*/

        override fun registerDataSetObserver(observer: DataSetObserver?) = Unit
        override fun unregisterDataSetObserver(observer: DataSetObserver?) = Unit


    }

    private class MarkAdapter(context: Context, private val list: List<MarkDao.MarkShortInfo>,
                              var onMarkClickListener: ((MarkDao.MarkShortInfo, View) -> Unit)? = null)
        : RecyclerView.Adapter<MarkAdapter.ViewHolder>() {
        private val inflater = LayoutInflater.from(context)!!

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(inflater.inflate(R.layout.mark_list_item, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also { info ->
                holder.markTitle.text = info.description.takeUnless { it.isBlank() } ?: "Bez tytułu"
                ViewCompat.setTransitionName(holder.markTitle, "ma$position")
                holder.markValue.text = when {
                    info.abbreviation != null -> info.abbreviation
                    info.markPointsValue != null -> info.markPointsValue.toString() trimEndToLength 4
                    else -> "Wut?"
                }
            }
        }

        override fun getItemCount() = list.size

        private fun onItemClicked(position: Int, view: View) {
            onMarkClickListener?.invoke(list[position], view)
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val markTitle = v.findViewById<TextView>(R.id.markTitle)!!
            val markValue = v.findViewById<TextView>(R.id.markValue)!!

            init {
                v.setOnClickListener { onItemClicked(adapterPosition, markTitle) }
            }
        }

        private infix fun String.trimEndToLength(@IntRange(from = 0L) maxLength: Int) = if (length <= maxLength) this else substring(0, maxLength + 1)
    }


}

