package jakubweg.mobishit.fragment

import android.app.DatePickerDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jakubweg.mobishit.R
import jakubweg.mobishit.model.TimetableModel
import java.lang.ref.WeakReference
import java.util.*

class TimetableFragment : Fragment() {

    companion object {
        fun newInstance() = TimetableFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_timetable, container, false)


    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this)[TimetableModel::class.java]
    }

    private var viewPager: ViewPager? = null
    private var tabs: TabLayout? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager = view.findViewById(R.id.dayViewPager)!!
        tabs = view.findViewById(R.id.dayTabs)!!

        viewModel.days.observe(this, Observer {
            it ?: return@Observer
            viewPager?.apply {
                adapter = DayViewPagerAdapter(it, fragmentManager!!)
                setCurrentItem(viewModel.currentSelectedDayIndex, false)
                tabs?.apply {
                    setupWithViewPager(viewPager, false)
                    addOnTabSelectedListener(tabSelectedListener)
                }
            }
        })
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
        private val fragment = WeakReference<TimetableFragment>(f)

        override fun onTabSelected(tab: TabLayout.Tab?) {
            fragment.get()?.viewModel?.currentSelectedDayIndex = tab?.position ?: return
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

        DatePickerDialog(requireContext(), DatePickerDialog.OnDateSetListener { _, newYear, newMonth, newDayOfMonth ->
            val millis = Calendar.getInstance().run {
                set(newYear, newMonth, newDayOfMonth - 1)
                timeInMillis
            }
            viewPager?.setCurrentItem((viewPager?.adapter as? DayViewPagerAdapter?)?.getIndexOfOrNext(millis)
                    ?: 0, true)
        }, year, month, dayOfMonth).show()
    }

    private class DayViewPagerAdapter(private val days: Array<TimetableModel.Date>,
                                      fm: FragmentManager)
    //: FragmentPagerAdapter(fm) {
        : FragmentStatePagerAdapter(fm) {
        override fun getCount() = days.size

        fun getIndexOfOrNext(time: Long): Int {
            val index = days.indexOfFirst { it.time >= time }
            return if (index == -1) 0 else index
        }

        fun getTime(index: Int): Long = days[index].time

        override fun getItem(index: Int): Fragment = OneDayFragment.newInstance(days[index].time)

        override fun getPageTitle(position: Int): CharSequence = days[position].formatted
    }
}
