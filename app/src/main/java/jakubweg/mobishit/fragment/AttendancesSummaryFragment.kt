package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AttendanceDao
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.AboutAttendancesModel
import jakubweg.mobishit.model.AttendancesModel
import jakubweg.mobishit.view.AttendanceBarView
import java.lang.ref.WeakReference

class AttendancesSummaryFragment : Fragment() {
    companion object {
        fun newInstance() = AttendancesSummaryFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attendance_stats, container, false)
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this)[AttendancesModel::class.java]
    }

    private val mainList get() = view?.findViewById<RecyclerView>(R.id.main_list)

    private val settingsListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AboutAttendancesModel.ACTION_SUBJECT_EXCLUDED_CHANGED) {
                (activity as? MainActivity)?.onNavigationItemSelected(R.id.nav_attendances, true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!MobiregPreferences.get(context!!).seenAboutAttendanceFragment) {
            view.findViewById<TextView?>(R.id.textAboutExcludingAttendances)?.apply {
                visibility = View.VISIBLE
                val drawable = AppCompatResources.getDrawable(context!!, R.drawable.ic_help)
                        ?.constantState?.newDrawable()?.also { it.mutate() }?.tintSelf(context
                        .themeAttributeToColor(android.R.attr.textColorTertiary))

                setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

                setOnClickListener {
                    AboutAttendancesFragment.newInstance()
                            .showSelf(activity)
                }
            }
        }

        mainList?.addItemDecoration(DividerItemDecoration(context!!, LinearLayoutManager.VERTICAL))

        viewModel.attendanceStats.observe(this, AttendancesObserver(this))
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(context!!)
                .registerReceiver(settingsListener, IntentFilter(
                        AboutAttendancesModel.ACTION_SUBJECT_EXCLUDED_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(context!!)
                .unregisterReceiver(settingsListener)
    }


    private fun onNewAttendances(list: List<AttendanceDao.AttendanceCountInfoHolder>) {
        mainList?.adapter = if (list.isEmpty())
            EmptyAdapter("Brak danych o obecnościach")
        else
            Adapter(context ?: return, list, clickListener)
    }


    private val clickListener = object : Adapter.ItemClickedListener {
        override fun onClicked(item: AttendanceDao.AttendanceCountInfoHolder) {
            AttendanceMonthFragment.newInstance(item.name, item.start, item.end)
                    .showSelf(activity)
        }
    }

    private class Adapter(context: Context,
                          private val list: List<AttendanceDao.AttendanceCountInfoHolder>,
                          private val onClickListener: ItemClickedListener)
        : RecyclerView.Adapter<Adapter.ViewHolder>() {

        interface ItemClickedListener {
            fun onClicked(item: AttendanceDao.AttendanceCountInfoHolder)
        }

        private val iconColor = context.themeAttributeToColor(android.R.attr.textColorTertiary)
        private val inflater = LayoutInflater.from(context)!!

        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.attendance_month_stat_list_item, parent, false), iconColor)
        }

        private val Float.formatMe get() = String.format("%.1f", times(100f))

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            list[pos].also { it ->
                holder.apply {
                    val total = it.totalRecords

                    title.precomputedText = it.name
                    summary.precomputedText = "W sumie $total"

                    val presents = (it.counts.find { it.countAs == "P" }?.count
                            ?: 0).also { count ->
                        presents.precomputedText = "Obecności\n$count (${(count / total.toFloat()).formatMe}%)"
                    }

                    val absents = (it.counts.find { it.countAs == "A" }?.count ?: 0).also { count ->
                        absents.precomputedText = "Nieobecności\n$count (${(count / total.toFloat()).formatMe}%)"
                        absents.setLeftDrawable(R.drawable.event_busy, iconColor)
                    }

                    val lateComings = (it.counts.find { it.countAs == "L" }?.count
                            ?: 0).also { count ->
                        lateComings.precomputedText = "Spóźnienia\n$count (${(count / total.toFloat()).formatMe}%)"
                    }

                    attendanceBar.setAttendanceData(presents, absents, lateComings)
                }
            }
        }

        private fun internalClickedHandler(pos: Int) {
            onClickListener.onClicked(list[pos])
        }

        private inner class ViewHolder(v: View, iconColor: Int) : RecyclerView.ViewHolder(v) {
            val title = v.textView(R.id.title)!!
            val summary = v.textView(R.id.sumary)!!
            val attendanceBar = v.findViewById<AttendanceBarView>(R.id.attendanceBar)!!

            val presents = v.textView(R.id.textPresents)!!
            val absents = v.textView(R.id.textAbsents)!!
            val lateComings = v.textView(R.id.textLateComings)!!

            init {
                presents.setLeftDrawable(R.drawable.event_available, iconColor)
                absents.setLeftDrawable(R.drawable.event_busy, iconColor)
                lateComings.setLeftDrawable(R.drawable.ic_assignment_late, iconColor)

                v.setOnClickListener { internalClickedHandler(adapterPosition) }
            }
        }
    }

    private class AttendancesObserver(f: AttendancesSummaryFragment)
        : Observer<List<AttendanceDao.AttendanceCountInfoHolder>> {
        private val fragment = WeakReference(f)
        override fun onChanged(it: List<AttendanceDao.AttendanceCountInfoHolder>?) {
            it ?: return
            fragment.get()?.onNewAttendances(it)
        }
    }
}
