package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.helper.EmptyAdapter
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.helper.setLeftDrawable
import jakubweg.mobishit.helper.textView
import jakubweg.mobishit.model.AttendanceDatesModel
import java.lang.ref.WeakReference

@Suppress("PrivatePropertyName")
class AttendanceDatesFragment : AttendanceBaseSheetFragment() {
    companion object {
        fun newInstance(title: String?,
                        start: Long,
                        end: Long,
                        subjectId: Int,
                        attendanceTypeId: Int):
                AttendanceDatesFragment {
            return AttendanceDatesFragment().apply {
                arguments = Bundle(3).apply {
                    putString("title", title)
                    putLongArray("params", longArrayOf(start, end, subjectId.toLong()))
                    putInt("type", attendanceTypeId)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attendance_dates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments!!.apply {
            val title = getString("title", null) ?: "Obecności"
            val params = getLongArray("params")!!
            val start = params[0]
            val end = params[1]
            val subjectId = params[2].toInt()
            val attendanceType = getInt("type")

            view.textView(R.id.hintClickLesson)?.apply {
                setLeftDrawable(R.drawable.nav_info, currentTextColor)
            }

            val viewModel = ViewModelProviders
                    .of(this@AttendanceDatesFragment)[AttendanceDatesModel::class.java]

            viewModel.init(start, end, subjectId, attendanceType)

            view.textView(R.id.title)?.precomputedText = title

            viewModel.days.observe(this@AttendanceDatesFragment, observer)
        }
    }

    private val observer = Observer<List<AttendanceDatesModel.AttendanceDayItem>> {
        view?.findViewById<RecyclerView>(R.id.attendanceDatesList)
                ?.adapter = if (it?.isEmpty() != false)
            EmptyAdapter("Brak dni z tym typem obecności")
        else
            Adapter(this@AttendanceDatesFragment, it)
    }

    private class Adapter(f: AttendanceDatesFragment,
                          private val list: List<AttendanceDatesModel.AttendanceDayItem>)
        : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val parent = WeakReference(f)
        private val inflater = LayoutInflater.from(f.context!!)!!
        private val TYPE_DAY_ITEM = 0
        private val TYPE_LESSON_ITEM = 1

        private fun onInternalItemClicked(pos: Int) {
            val item = list[pos]
            val activity = parent.get()?.apply {
                dismissWithoutOpeningPrevious()
            }?.activity as? MainActivity ?: return

            activity.startActivity(Intent(activity, MainActivity::class.java).also {
                it.action = MainActivity.ACTION_SHOW_TIMETABLE
                it.putExtra("id", item.date.div(1000L).toInt())
            })
        }

        override fun getItemCount() = list.size

        override fun getItemViewType(position: Int) = if (list[position].isDayElement) TYPE_DAY_ITEM else TYPE_LESSON_ITEM

        override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(
                    when (type) {
                        TYPE_DAY_ITEM -> R.layout.attendance_date_day_list_item
                        TYPE_LESSON_ITEM -> R.layout.attendance_date_lesson_list_item
                        else -> throw IllegalArgumentException()
                    }
                    , viewGroup, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            list[pos].also { item ->
                holder.title.precomputedText = item.title
            }
        }

        private inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.textView(R.id.title)!!

            init {
                v.setOnClickListener { onInternalItemClicked(adapterPosition) }
            }
        }
    }
}
