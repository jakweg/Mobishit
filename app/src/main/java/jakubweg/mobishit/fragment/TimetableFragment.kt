package jakubweg.mobishit.fragment

import android.app.DatePickerDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.DateHelper
import jakubweg.mobishit.model.TimetableModel
import java.lang.ref.WeakReference
import java.util.*

class TimetableFragment : Fragment() {

    companion object {
        fun newInstance() = TimetableFragment()

        private var mRequestedDate = -1L
        var requestedDate
            get() = mRequestedDate.div(1000L).toInt()
            set(value) {
                mRequestedDate = value.toLong().times(1000L)
            }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_timetable, container, false)


    //private lateinit var viewModel: TimetableModel
    private val viewModel get() = ViewModelProviders.of(this)[TimetableModel::class.java]

    private var viewPager: ViewPager? = null
    private var tabs: DaysTabLayout? = null
    private var scrollSmoothly = true
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager = view.findViewById(R.id.dayViewPager)!!
        tabs = view.findViewById(R.id.dayTabs)!!

        viewPager?.apply {
            adapter = DayViewPagerAdapter(this@TimetableFragment, childFragmentManager)
            tabs?.apply {
                setupWithViewPager(viewPager, true)
            }
        }

        viewModel.days.observe(this, Observer {
            it ?: return@Observer
            viewPager?.apply {
                (adapter as? DayViewPagerAdapter?)?.apply {
                    tabs?.removeOnTabSelectedListener(tabSelectedListener)
                    setDays(it)
                    postOnMainLooper {
                        visibility = View.VISIBLE
                        shouldReturnEmptyFragments = false
                        setCurrentItem(viewModel.currentSelectedDayIndex, scrollSmoothly)
                        scrollSmoothly = false
                        tabs?.addOnTabSelectedListener(tabSelectedListener)
                    }
                }
            }
        })

        if (viewModel.days.value == null) {
            if (savedInstanceState == null || !savedInstanceState.containsKey("currentDay")) {
                if (requestedDate > 0) {
                    viewModel.requestDate(requestedDate.toLong() * 1000L)
                    requestedDate = -1
                } else {
                    val calendar = Calendar.getInstance()!!
                    val requestDate = calendar.timeInMillis / DateHelper.MILLIS_IN_DAY * DateHelper.MILLIS_IN_DAY + when {
                        calendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY -> 2 * DateHelper.MILLIS_IN_DAY
                        calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY -> DateHelper.MILLIS_IN_DAY
                        calendar[Calendar.HOUR_OF_DAY] >= 17 -> DateHelper.MILLIS_IN_DAY //at friday this should skip to monday... but who bothers about school at friday??
                        else -> 0
                    }
                    viewModel.requestDate(requestDate)
                }
            } else {
                viewModel.requestDate(savedInstanceState.getLong("currentDay"))
            }
        } else {
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //super.onSaveInstanceState(outState)
        viewModel.days.value?.takeIf { it.isNotEmpty() }?.also {
            if (viewModel.currentSelectedDayIndex in 0 until it.size)
                outState.putLong("currentDay", it[viewModel.currentSelectedDayIndex].time)
        }
    }

    private inline fun postOnMainLooper(crossinline function: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({ function() }, 50L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
        tabs?.removeOnTabSelectedListener(tabSelectedListener)
        tabs = null
    }


    private val tabSelectedListener = TabSelectedListener(this)

    private class TabSelectedListener(f: TimetableFragment)
        : TabLayout.OnTabSelectedListener {
        var ignoreNextSelection = false
        private val fragment = WeakReference<TimetableFragment>(f)

        override fun onTabSelected(tab: TabLayout.Tab?) {
            if (ignoreNextSelection) {
                ignoreNextSelection = false
                return
            }
            val position = tab?.position ?: return
            fragment.get()?.viewModel?.currentSelectedDayIndex = position
        }

        override fun onTabReselected(p0: TabLayout.Tab?) = Unit
        override fun onTabUnselected(p0: TabLayout.Tab?) = Unit
    }

    fun onChooseDateClicked() {
        val (year, month, dayOfMonth) = Calendar.getInstance().run {
            timeInMillis = (viewPager?.adapter as? DayViewPagerAdapter?)
                    ?.getTime(viewPager?.currentItem ?: return) ?: 0L

            return@run Triple(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
        }

        DatePickerDialog(context!!, DatePickerDialog.OnDateSetListener { _, newYear, newMonth, newDayOfMonth ->
            val millis = Calendar.getInstance().run {
                set(newYear, newMonth, newDayOfMonth - 1)
                timeInMillis
            }
            val index = (viewPager?.adapter as? DayViewPagerAdapter?)?.getIndexOfOrNext(millis)
            if (index != null)
                viewPager?.setCurrentItem(index, true)
            else {
                viewModel.requestDate(millis)
                Handler(Looper.getMainLooper()).postDelayed({
                    val index2 = (viewPager?.adapter as? DayViewPagerAdapter?)?.getIndexOfOrNext(millis)
                    if (index2 != null) {
                        viewPager?.setCurrentItem(index2, true)
                    } else {
                        Toast.makeText(context!!, "Dzień poza moim zasięgiem \uD83D\uDE1E", Toast.LENGTH_SHORT).show()
                    }
                }, 300L)
            }
        }, year, month, dayOfMonth).show()
    }

    class DaysTabLayout : TabLayout {
        private var adapter: DayViewPagerAdapter? = null

        constructor(context: Context?)
                : super(context)
        constructor(context: Context?, attrs: AttributeSet?)
                : super(context, attrs)
        constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
                : super(context, attrs, defStyleAttr)

        override fun setupWithViewPager(viewPager: ViewPager?) {
            onSetupWithViewPager(viewPager)
            super.setupWithViewPager(viewPager)
        }

        override fun setupWithViewPager(viewPager: ViewPager?, autoRefresh: Boolean) {
            onSetupWithViewPager(viewPager)
            super.setupWithViewPager(viewPager, autoRefresh)
        }

        private fun onSetupWithViewPager(viewPager: ViewPager?){
            adapter = viewPager?.adapter as DayViewPagerAdapter?
        }

        override fun addTab(tab: Tab, position: Int, setSelected: Boolean) {
            super.addTab(tab, position, setSelected)
            val tabCustomView = AppCompatTextView(context)
            tabCustomView.gravity = Gravity.CENTER
            tabCustomView.setTextColor(tabTextColors)
            adapter?.also {
                tabCustomView.text = tab.text
                if(it.getTime(position) / DateHelper.MILLIS_IN_DAY == DateHelper.getNowDateMillis() / DateHelper.MILLIS_IN_DAY)
                    tabCustomView.setTextColor(ContextCompat.getColorStateList(context, R.color.today_timetable_tab_color))
            }
            TextViewCompat.setTextAppearance(tabCustomView, R.style.TimetableDayTab)

            tab.customView = tabCustomView
        }
    }

    private class DayViewPagerAdapter(f: TimetableFragment?,
                                      fm: FragmentManager)
        : FragmentStatePagerAdapter(fm) {
        var shouldReturnEmptyFragments = true
        private val parent = WeakReference(f)
        private var days = arrayListOf<TimetableModel.Date>()

        fun setDays(days: ArrayList<TimetableModel.Date>) {
            this.days.clear()
            this.days.addAll(days)
            notifyDataSetChanged()
        }

        override fun getCount() = days.size

        fun getIndexOfOrNext(time: Long): Int? {
            if (days.isNotEmpty() && time in days.first().time..days.last().time) {
                val index = days.indexOfFirst { it.time >= time }
                return if (index == -1) 0 else index
            }
            return null
        }

        fun getTime(index: Int): Long {
            if (days.isEmpty())
                return System.currentTimeMillis()
            return days[index].time
        }

        override fun getItem(index: Int): Fragment? {
            if (shouldReturnEmptyFragments)
                return Fragment()
            if (index == 0 && days.first().isActionButton) {
                parent.get()?.apply {
                    tabSelectedListener.ignoreNextSelection = true
                    viewModel.requestDate(days.first().time)
                }
            } else if (index == days.size - 1 && days.last().isActionButton) {
                parent.get()?.apply {
                    tabSelectedListener.ignoreNextSelection = true
                    viewModel.requestDate(days.last().time)
                }
            }
            return OneDayFragment.newInstance(days[index].time)
        }

        override fun getPageTitle(position: Int): CharSequence = days[position].formatted
    }
}
