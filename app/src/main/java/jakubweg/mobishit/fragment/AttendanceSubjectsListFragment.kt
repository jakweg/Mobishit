package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jakubweg.mobishit.R
import jakubweg.mobishit.db.AttendanceDao
import jakubweg.mobishit.helper.EmptyAdapter
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.helper.textView
import jakubweg.mobishit.model.AttendanceSubjectsListModel
import java.lang.ref.WeakReference

class AttendanceSubjectsListFragment : AttendanceBaseSheetFragment() {
    companion object {
        fun newInstance(title: String, start: Long, end: Long): AttendanceSubjectsListFragment {
            return AttendanceSubjectsListFragment().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putLong("start", start)
                    putLong("end", end)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attendance_subjects_list, container, false)
    }

    private lateinit var viewModel: AttendanceSubjectsListModel
    private var dateStart = 0L
    private var dateEnd = 0L
    private var title = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this)[AttendanceSubjectsListModel::class.java]

        arguments!!.apply {
            dateStart = getLong("start")
            dateEnd = getLong("end")
            title = getString("title") ?: ""

            view.textView(R.id.title)?.precomputedText = "Przedmioty • $title"
            viewModel.init(dateStart, dateEnd)

            view.findViewById<RecyclerView>(R.id.subjectsList)
                    ?.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))

            viewModel.subjects.observe(this@AttendanceSubjectsListFragment,
                    Observer {
                        onNewData(it ?: return@Observer)
                    })
        }
    }

    private fun onNewData(list: List<AttendanceDao.AttendanceSubjectInfo>) {
        view?.findViewById<RecyclerView>(R.id.subjectsList)
                ?.adapter = if (list.isEmpty())
            EmptyAdapter("Brak danych")
        else
            Adapter(this, context!!, list)
    }

    private fun onItemClicked(item: AttendanceDao.AttendanceSubjectInfo) {
        AttendanceMonthFragment.newInstance("${item.subjectName} • $title"
                , dateStart, dateEnd, item.subjectId)
                .showSelfInsteadOfMe(this)
    }

    private class Adapter(fragment: AttendanceSubjectsListFragment?, context: Context,
                          private val list: List<AttendanceDao.AttendanceSubjectInfo>)
        : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val parent = WeakReference(fragment)
        private val inflater = LayoutInflater.from(context)!!
        override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.attendance_subject_list_item, viewGroup, false))
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            list[pos].also {
                holder.subjectName.precomputedText = it.subjectName
                holder.percent.text = String.format("%.1f%% (%d)", it.percentage, it.total)
            }
        }

        private fun internalOnClickListener(pos: Int) {
            parent.get()?.onItemClicked(list[pos])
        }

        private inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subjectName = v.textView(R.id.subject_name)!!
            val percent = v.textView(R.id.percent)!!

            init {
                v.setOnClickListener { internalOnClickListener(adapterPosition) }
            }
        }
    }
}
