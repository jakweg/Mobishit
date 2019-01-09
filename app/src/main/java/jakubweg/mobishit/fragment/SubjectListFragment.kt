package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.activity.MarkOptionsListener
import jakubweg.mobishit.db.AverageCacheData
import jakubweg.mobishit.db.LastMarkCacheData
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.SubjectListModel
import jakubweg.mobishit.view.MarksListView
import java.lang.ref.WeakReference


class SubjectListFragment : Fragment(), MarksViewOptionsFragment.OptionsChangedListener {
    companion object {
        fun newInstance() = SubjectListFragment()
    }

    override fun onOtherOptionsChanged() = Unit
    override fun onTermChanged() {
        viewModel.requestSubjectsAfterTermChanges()
    }

    private lateinit var listener: MarkOptionsListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        listener = MarkOptionsListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_list_subjects, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainList = view.findViewById<RecyclerView>(R.id.main_list)!!


        val dividerItemDecoration = DividerItemDecoration(mainList.context,
                (mainList.layoutManager as LinearLayoutManager).orientation)
        mainList.addItemDecoration(dividerItemDecoration)

        if (MobiregPreferences.get(context!!).showLastMarks) {
            val lastMarksList = view.findViewById<RecyclerView>(R.id.lastMarksList)!!
            view.findViewById<TextView?>(R.id.lastMarksTitle)?.also {
                it.setLeftDrawable(R.drawable.ic_expand_more)
                it.setOnClickListener {
                    viewModel.onClickedExpand()
                }
            }
            viewModel.lastMarks.observe(this,
                    SafeLastMarksObserver(this))
            lastMarksList.addItemDecoration(dividerItemDecoration)
        }

        viewModel.subjects.observe(this,
                SafeSubjectsObserver(mainList))

        if (mainList.adapter == null)
            mainList.adapter = EmptyAdapter("Ładowanie danych...")

    }

    override fun onStop() {
        super.onStop()
        viewModel.scrollPosition = view?.findViewById<NestedScrollView?>(R.id.scrollView)?.scrollY ?: 0
    }

    override fun onStart() {
        super.onStart()
        Handler(Looper.getMainLooper()).post {
            view?.findViewById<NestedScrollView?>(R.id.scrollView)?.scrollTo(0, viewModel.scrollPosition)
        }
    }

    private class SafeLastMarksObserver(v: SubjectListFragment)
        : Observer<List<LastMarkCacheData>> {
        private val fragment = WeakReference<SubjectListFragment>(v)

        override fun onChanged(list: List<LastMarkCacheData>?) {
            this.fragment.get()?.apply {
                if (list.isNullOrEmpty()) {
                    view?.findViewById<View?>(R.id.lastMarksLayout)?.visibility = View.GONE
                } else {
                    view?.findViewById<View?>(R.id.lastMarksLayout)?.visibility = View.VISIBLE
                    val lastMarksList = view?.findViewById<RecyclerView?>(R.id.lastMarksList)
                            ?: return
                    lastMarksList.adapter.also {
                        if (it != null && it is LastMarksAdapter) {
                            it.setNewMarksList(list)
                        } else
                            lastMarksList.adapter = LastMarksAdapter(this, list)
                    }
                }
            }
        }
    }

    private fun onLastMarkClicked(markId: Int) {
        MarkDetailsFragment.newInstance(markId)
                .showSelf(activity)
    }

    private class LastMarksAdapter(fragment: SubjectListFragment,
                                   private var list: List<LastMarkCacheData>)
        : RecyclerView.Adapter<LastMarksAdapter.ViewHolder>() {

        fun setNewMarksList(list: List<LastMarkCacheData>) {
            val oldList = this.list
            this.list = list
            when {
                list.size > oldList.size -> notifyItemRangeInserted(oldList.size, list.size - oldList.size)
                list.size < oldList.size -> notifyItemRangeRemoved(list.size, oldList.size - list.size)
                else -> notifyDataSetChanged()
            }
        }

        private val weakFragment = WeakReference(fragment)
        private val inflater = LayoutInflater.from(fragment.context!!)!!
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.last_mark_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            list[pos].also {
                holder.title.precomputedText = it.description
                holder.markValue.precomputedText = if (it.value.contains('.'))
                    it.value.replace('.', ',') else it.value
            }
        }

        private fun onItemClicked(pos: Int) {
            weakFragment.get()?.onLastMarkClicked(list[pos].id)
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.findViewById<TextView>(R.id.title)!!
            val markValue = v.findViewById<TextView>(R.id.markValue)!!

            init {
                v.setOnClickListener { onItemClicked(adapterPosition) }
            }
        }
    }


    private class SafeSubjectsObserver(v: RecyclerView)
        : Observer<List<AverageCacheData>> {

        private val mainList = WeakReference<RecyclerView>(v)

        override fun onChanged(it: List<AverageCacheData>?) {
            val mainList = this.mainList.get() ?: return
            mainList.setHasFixedSize(true)
            if (it?.isEmpty() != false)
                mainList.adapter = EmptyAdapter("Brak przedmiotów z których masz oceny w wybranym semestrze.\n" +
                        "Kliknij ikonę powyżej, aby zmienić semestr")
            else
                mainList.adapter = SubjectAdapter(mainList.context!!, it).apply {
                    onSubjectClicked = { subject, view, _ ->
                        (mainList.context as? DoublePanelActivity)
                                ?.applyNewDetailsFragment(view, SubjectsMarkFragment.newInstance(
                                        subject.subjectId, subject.subjectName ?: "",
                                        ViewCompat.getTransitionName(view)))
                    }
                }
        }
    }

    private inline val viewModel
        get()
        = ViewModelProviders.of(this).get(SubjectListModel::class.java)


    private class SubjectAdapter(context: Context,
                                 private val list: List<AverageCacheData>)
        : RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)!!
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : ViewHolder = ViewHolder(inflater.inflate(R.layout.subject_list_item, parent, false))

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also {
                holder.subjectName.text = it.subjectName
                ViewCompat.setTransitionName(holder.subjectName, "sn$position")
                holder.averageText.text = it.shortAverageText
                holder.markList.setDisplayedMarks(it.getMarksList())
            }
        }

        var onSubjectClicked: ((AverageCacheData, View, Int) -> Unit)? = null

        private fun onViewClicked(pos: Int, view: View) {
            onSubjectClicked?.invoke(list[pos], view, pos)
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subjectName = v.textView(R.id.subject_name)!!
            val averageText = v.textView(R.id.average_text)!!
            val markList = v.findViewById<MarksListView>(R.id.marks_list)!!

            init {
                v.setOnClickListener { onViewClicked(adapterPosition, subjectName) }
            }
        }
    }
}
