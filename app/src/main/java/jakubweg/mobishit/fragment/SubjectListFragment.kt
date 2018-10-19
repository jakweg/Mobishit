package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.EmptyAdapter
import jakubweg.mobishit.model.SubjectListModel
import jakubweg.mobishit.view.MarksListView
import java.lang.ref.WeakReference


class SubjectListFragment : Fragment() {
    companion object {
        fun newInstance() = SubjectListFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_list_subjects, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainList = view.findViewById<RecyclerView>(R.id.main_list)!!

        viewModel.subjects.observe(this,
                InternalObserver(mainList))

        val dividerItemDecoration = DividerItemDecoration(mainList.context,
                (mainList.layoutManager as LinearLayoutManager).orientation)
        mainList.addItemDecoration(dividerItemDecoration)
        if (mainList.adapter == null)
            mainList.adapter = EmptyAdapter("Ładowanie danych...")

    }

    private class InternalObserver(v: RecyclerView)
        : Observer<List<MarkDao.SubjectShortInfo>> {

        private val mainList = WeakReference<RecyclerView>(v)

        override fun onChanged(it: List<MarkDao.SubjectShortInfo>?) {
            val mainList = this.mainList.get() ?: return
            if (it?.isEmpty() != false)
                mainList.adapter = EmptyAdapter("Brak przedmiotów z których masz oceny")
            else
                mainList.adapter = SubjectAdapter(mainList.context!!, it).apply {
                    onSubjectClicked = { subject, view, _ ->
                        (mainList.context as? DoublePanelActivity)
                                ?.applyNewDetailsFragment(view, SubjectsMarkFragment.newInstance(subject.id, subject.name,
                                        ViewCompat.getTransitionName(view)))
                    }
                }
        }
    }

    private inline val viewModel
        get()
        = ViewModelProviders.of(this).get(SubjectListModel::class.java)


    private class SubjectAdapter(context: Context, private val list: List<MarkDao.SubjectShortInfo>)
        : RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)!!
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : ViewHolder = ViewHolder(inflater.inflate(R.layout.subject_list_item, parent, false))

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also {
                holder.subjectName.text = it.name
                ViewCompat.setTransitionName(holder.subjectName, "sn$position")
                holder.averageText.text = it.averageText
                holder.markList.setDisplayedMarks(it.subjectsMarks)
            }
        }

        var onSubjectClicked: ((MarkDao.SubjectShortInfo, View, Int) -> Unit)? = null

        private fun onViewClicked(pos: Int, view: View) {
            onSubjectClicked?.invoke(list[pos], view, pos)
        }

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subjectName = v.findViewById<TextView>(R.id.subject_name)!!
            val averageText = v.findViewById<TextView>(R.id.average_text)!!
            val markList = v.findViewById<MarksListView>(R.id.marks_list)!!

            init {
                v.setOnClickListener { onViewClicked(adapterPosition, subjectName) }
            }
        }
    }
}
