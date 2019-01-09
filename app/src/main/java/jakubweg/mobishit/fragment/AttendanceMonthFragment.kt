package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import jakubweg.mobishit.R
import jakubweg.mobishit.db.AttendanceDao
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.helper.setLeftDrawable
import jakubweg.mobishit.helper.textView
import jakubweg.mobishit.model.AttendanceMonthModel
import jakubweg.mobishit.view.AttendanceBarView
import java.lang.ref.WeakReference

class AttendanceMonthFragment : AttendanceBaseSheetFragment() {
    companion object {
        fun newInstance(name: String?, start: Long, end: Long, subjectId: Int): AttendanceMonthFragment {
            return AttendanceMonthFragment().apply {
                arguments = Bundle().also {
                    it.putString("name", name)
                    it.putLong("start", start)
                    it.putLong("end", end)
                    it.putInt("subject", subjectId)
                }
            }
        }

        fun newInstance(name: String?, start: Long, end: Long) = newInstance(name, start, end, -1)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attendance_month, container, false)
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this)[AttendanceMonthModel::class.java]
    }

    private var start = -1L
    private var end = -1L
    private var title = ""
    private var subjectId = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments!!.also { args ->
            start = args.getLong("start", -1L)
            end = args.getLong("end", -1L)
            title = args.getString("name", null)
                    ?: "Statystyki obecności"
            subjectId = args.getInt("subject", -1)

            if (!MobiregPreferences.get(context!!).seenAttendanceDates) {
                view.textView(R.id.hintClickAttendanceType)?.also {
                    it.visibility = View.VISIBLE
                    it.setLeftDrawable(R.drawable.nav_info, it.currentTextColor)
                }
            }


            viewModel.init(start, end, subjectId)

            view.textView(R.id.title)?.precomputedText = title
            view.findViewById<Button>(R.id.btnSubjectStats)?.apply {
                if (subjectId >= 0)
                    visibility = View.GONE
                setOnClickListener {
                    AttendanceSubjectsListFragment
                            .newInstance(title, start, end)
                            .showSelfInsteadOfMe(this@AttendanceMonthFragment)
                }
            }
        }


        val weakView = WeakReference(view)

        viewModel.attendanceTypes.observe(this, Observer { data ->
            data ?: return@Observer
            weakView.get()?.apply {
                findViewById<AttendanceBarView>(R.id.attendanceBar)?.setAttendanceData(data.map { it.color to it.count })
                findViewById<RecyclerView>(R.id.attendanceTypeList)?.adapter =
                        AttendanceTypesAdapter(this@AttendanceMonthFragment, context!!, data)
            }
        })
    }

    private fun onAttendanceClicked(item: AttendanceDao.AttendanceTypeAndCountInfo) {
        AttendanceDatesFragment
                .newInstance("${item.name} \u2022 $title", start, end, subjectId, item.id)
                .showSelfInsteadOfMe(this)
    }

    private class AttendanceTypesAdapter(f: AttendanceMonthFragment?,
                                         context: Context,
                                         private val list: List<AttendanceDao.AttendanceTypeAndCountInfo>)
        : RecyclerView.Adapter<AttendanceTypesAdapter.ViewHolder>() {
        private val fragment = WeakReference(f)

        private val inflater = LayoutInflater.from(context)!!

        override fun getItemCount() = list.size

        override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.attendance_type_list_item, viewGroup, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            list[pos].also {
                holder.colorView.setBackgroundColor(it.color)
                holder.title.precomputedText = it.name
                holder.percentage.precomputedText = it.count.toString()
            }
        }

        private fun internalOnClick(pos: Int) {
            fragment.get()?.onAttendanceClicked(list[pos])
        }

        private inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val colorView = v.findViewById<View>(R.id.colorView)!!

            val title = v.textView(R.id.title)!!
            val percentage = v.textView(R.id.textPresents)!!

            init {
                v.setOnClickListener { internalOnClick(adapterPosition) }
            }
        }
    }
}