package jakubweg.mobishit.activity

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.*
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.internal.view.SupportMenuItem
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.fragment.*
import jakubweg.mobishit.helper.DateHelper
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.SnackbarController
import jakubweg.mobishit.service.CountdownService
import jakubweg.mobishit.service.UpdateWorker
import java.lang.ref.WeakReference
import java.util.*

abstract class BaseLifecycleAwareObject
(protected val lifecycle: Lifecycle) : LifecycleObserver {

    private var needsToTakeAction = false

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (needsToTakeAction)
            doOnVisible()
    }

    protected fun requestTaskOnStart() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            doOnVisible()
        else
            needsToTakeAction = true
    }

    abstract fun doOnVisible()
}

class MainActivityLoginListener(activity: MainActivity?) :
        BaseLifecycleAwareObject(activity!!.lifecycle) {
    private val weakActivity = WeakReference(activity)

    private val listener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            sharedPreferences ?: return
            if (key == "isSignedIn") {
                if (!sharedPreferences.getBoolean(key, false)) {
                    val act = weakActivity.get()
                    if (act == null) {
                        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
                    }
                    requestTaskOnStart()
                }
            }
        }
    }

    init {
        activity?.also {
            MobiregPreferences.get(it)
                    .registerChangeListener(listener)
        }
        lifecycle.addObserver(this)
    }

    override fun doOnVisible() {
        val act = weakActivity.get() ?: return
        act.finish()
        Toast.makeText(act, "Wylogowano", Toast.LENGTH_SHORT).show()
    }
}


class MainActivityNavigationLayoutUtils(activity: MainActivity)
    : BaseLifecycleAwareObject(activity.lifecycle) {

    private val weakActivity = WeakReference(activity)

    private var selectedItem: MenuItem? = null
    private var notifyActivityInstant = false
    private var tellActivityToRequestNewFragment = true
    private val listener = NavigationView.OnNavigationItemSelectedListener {
        if (it.isCheckable)
            selectedItem = it
        else {
            notifyActivityAboutNewSelectedItem(it)
        }
        weakActivity.get()?.also { act ->
            act.findViewById<DrawerLayout>(R.id.drawer_layout)
                    ?.closeDrawer(GravityCompat.START)
            if (it.isCheckable) {
                adjustToSelectedItem(act, it.itemId)
                if (notifyActivityInstant) {
                    notifyActivityAboutNewSelectedItem()
                    notifyActivityInstant = false
                } else {
                    Handler(act.mainLooper ?: return@also)
                            .postDelayed(::notifyActivityAboutNewSelectedItem, 450L)
                }
            }
        }

        true
    }

    private fun adjustToSelectedItem(activity: MainActivity, itemId: Int) {
        setTitleById(activity, itemId)
        setToolbarItemsVisibleById(activity, itemId)
    }

    private fun notifyActivityAboutNewSelectedItem() {
        notifyActivityAboutNewSelectedItem(selectedItem ?: return)
    }

    private fun notifyActivityAboutNewSelectedItem(item: MenuItem) {
        weakActivity.get()?.also { activity ->
            activity.onNavigationItemSelected(item.itemId, tellActivityToRequestNewFragment)
        }
        tellActivityToRequestNewFragment = true
    }

    init {
        lifecycle.addObserver(this)
        val navigationView = activity.navigationView
        navigationView.setNavigationItemSelectedListener(listener)

        val drawerLayout = activity.drawerLayout
        val toolbar = activity.toolbar

        val toggle = ActionBarDrawerToggle(
                activity, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        activity.menuInflater.inflate(R.menu.activity_main_toolbar, toolbar.menu)
        if (MobiregPreferences.get(activity).getAppUpdateLink() != null)
            navigationView.menu?.findItem(R.id.nav_app_update)?.isVisible = true
        toolbar.setOnMenuItemClickListener(::onMenuItemClicked)
    }

    fun handleIntent(activity: MainActivity, intent: Intent, savedInstanceState: Bundle?) {
        val navigationView = activity.navigationView

        val idFromIntent = intent.getIntExtra("id", 0)

        var checkItemId: Int? = null
        var detailsFragment: Fragment? = null
        var requestNewMainLayout = true
        var forceRecheck = false
        if (savedInstanceState != null) {
            checkItemId = savedInstanceState.getInt("currentSelectedItemId", 0).takeUnless { it == 0 }
            requestNewMainLayout = false
        } else {
            intent.action?.also {
                when (it) {
                    MainActivity.ACTION_SHOW_TIMETABLE -> {
                        if (idFromIntent > 0) {
                            TimetableFragment.requestedDate = idFromIntent
                            forceRecheck = true
                        } else TimetableFragment.requestedDate = -1
                        checkItemId = R.id.nav_timetable
                    }
                    MainActivity.ACTION_SHOW_MARK -> {
                        if (idFromIntent != 0)
                            MarkDetailsFragment.newInstance(idFromIntent)
                                    .showSelf(activity)
                        checkItemId = R.id.nav_marks
                    }
                    MainActivity.ACTION_SHOW_MESSAGE -> {
                        if (idFromIntent != 0)
                            detailsFragment = MessageDetailsFragment.newInstance(idFromIntent)
                        checkItemId = R.id.nav_messages
                    }
                    MainActivity.ACTION_SHOW_ATTENDANCE_STATS -> checkItemId = R.id.nav_attendances
                    MainActivity.ACTION_SHOW_COMPARISONS -> checkItemId = R.id.nav_comparisons
                    MainActivity.ACTION_SHOW_PREFERENCES -> checkItemId = R.id.nav_settings
                    MainActivity.ACTION_ABOUT_APP -> checkItemId = R.id.nav_about
                    MainActivity.ACTION_CALCULATE_AVERAGE -> checkItemId = R.id.nav_calculate_average
                    MainActivity.ACTION_UPDATE_PASSWORD -> {
                        checkItemId = R.id.nav_settings
                        navigationView.postDelayed(::showUpdatePasswordDialog, 500)
                    }
                    MainActivity.ACTION_REFRESH_NOW -> {
                        activity.tryToRefresh()
                    }
                }
            }
        }

        tellActivityToRequestNewFragment = requestNewMainLayout
        checkItemId?.also {
            val item = navigationView.menu.findItem(it)
            if (!item.isChecked || forceRecheck) {
                navigationView.setCheckedItem(item)
                notifyActivityInstant = true
                listener.onNavigationItemSelected(item)
            }
        }

        if (navigationView.checkedItem == null) {
            navigationView.setCheckedItem(getDefaultLaunchScreenItemId(activity))
            listener.onNavigationItemSelected(navigationView.checkedItem!!)
        }

        detailsFragment?.also { activity.applyNewDetailsFragment(it) }
    }

    private fun showUpdatePasswordDialog() {
        try {
            weakActivity.get()
                    ?.getLastFragment<GeneralPreferenceFragment>()
                    ?.showUpdatePasswordDialog()
        } catch (e: Exception) {
        }
    }

    private fun getDefaultLaunchScreenItemId(activity: MainActivity): Int {
        return when (MobiregPreferences.get(activity).defaultFragment) {
            MainActivity.FRAGMENT_MARKS -> R.id.nav_marks
            MainActivity.FRAGMENT_TIMETABLE -> R.id.nav_timetable
            MainActivity.FRAGMENT_MESSAGES -> R.id.nav_messages
            else -> R.id.nav_marks
        }
    }

    override fun doOnVisible() {

    }

    private fun onMenuItemClicked(it: MenuItem): Boolean {
        weakActivity.get()?.onToolbarItemClicked(it.itemId)
        return true
    }

    private fun setTitleById(activity: MainActivity, itemId: Int) {
        activity.toolbar.title = when (itemId) {
            R.id.nav_marks -> "Oceny"
            R.id.nav_messages -> "Wiadomości i uwagi"
            R.id.nav_timetable -> "Plan lekcji"
            R.id.nav_comparisons -> "Porównania"
            R.id.nav_attendances -> "Obecności"
            R.id.nav_tests -> "Sprawdziany"
            R.id.nav_about -> "O aplikacji"
            R.id.nav_settings -> "Ustawienia"
            R.id.nav_calculate_average -> "Kalkulator średnich"
            else -> return
        }
    }

    private fun setToolbarItemsVisibleById(activity: MainActivity, itemId: Int) {
        /*val visibleItems = when (itemId) {
            R.id.nav_marks -> intArrayOf(R.id.nav_sort_by)
            R.id.nav_timetable -> intArrayOf(R.id.nav_choose_date)
            R.id.nav_attendances -> intArrayOf(R.id.nav_about_attendances)
            R.id.nav_comparisons -> intArrayOf(R.id.nav_sort_by)
            else -> intArrayOf()
        }

        activity.toolbar.menu!!.forEach {
            it.isVisible = (it.itemId in visibleItems)
            it.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_ALWAYS)
        }*/
        val visibleItem = when (itemId) {
            R.id.nav_marks -> (R.id.nav_sort_by)
            R.id.nav_timetable -> (R.id.nav_choose_date)
            R.id.nav_attendances -> (R.id.nav_about_attendances)
            R.id.nav_comparisons -> (R.id.nav_sort_by)
            else -> R.id.none
        }

        activity.toolbar.menu!!.forEach {
            it.isVisible = (it.itemId == visibleItem)
            it.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }
}


class MarkOptionsListener(private val fragment: Fragment,
                          private val listener: MarksViewOptionsFragment.OptionsChangedListener)
    : BaseLifecycleAwareObject(fragment.lifecycle) {

    companion object {
        private const val ACTION_MARK_OPTIONS_CHANGED = "markOptionsChanged"
        private const val EXTRA_CHANGED_TERM = "term"
        private const val EXTRA_CHANGED_ORDER = "order"
        private const val EXTRA_CHANGED_GROUPING = "grouping"

        fun notifyOptionsChanged(context: Context?,
                                 changedTerm: Boolean,
                                 changedOrder: Boolean,
                                 changedGrouping: Boolean) {
            if (!changedTerm && !changedOrder && !changedGrouping)
                return
            if (changedTerm)
                MobiregPreferences.get(context).hasReadyAverageCache = false

            LocalBroadcastManager
                    .getInstance(context ?: return)
                    .sendBroadcast(Intent()
                            .apply {
                                action = ACTION_MARK_OPTIONS_CHANGED
                                putExtra(EXTRA_CHANGED_TERM, changedTerm)
                                putExtra(EXTRA_CHANGED_ORDER, changedOrder)
                                putExtra(EXTRA_CHANGED_GROUPING, changedGrouping)
                            })
        }
    }

    constructor(any: Any) : this(
            (any as? Fragment)!!,
            (any as? MarksViewOptionsFragment.OptionsChangedListener)!!)

    init {
        fragment.lifecycle.addObserver(this)
    }

    private var intentToHandle: Intent? = null
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intentToHandle = intent ?: return
            requestTaskOnStart()
        }
    }

    private val intentFilter
        get() = IntentFilter(ACTION_MARK_OPTIONS_CHANGED)

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        LocalBroadcastManager
                .getInstance(fragment.context ?: return)
                .registerReceiver(broadcastReceiver, intentFilter)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        LocalBroadcastManager
                .getInstance(fragment.context ?: return)
                .unregisterReceiver(broadcastReceiver)
    }

    override fun doOnVisible() {
        intentToHandle?.apply {
            when (action ?: return) {
                ACTION_MARK_OPTIONS_CHANGED -> listener.onOptionsChanged(
                        getBooleanExtra(EXTRA_CHANGED_TERM, false),
                        getBooleanExtra(EXTRA_CHANGED_ORDER, false),
                        getBooleanExtra(EXTRA_CHANGED_GROUPING, false)
                )
            }
        }
    }
}


class MainActivitySyncObserver(
        parentActivity: MainActivity?,
        tryToLaunchCountdownService: Boolean)
    : BaseLifecycleAwareObject(parentActivity!!.lifecycle) {

    private val weakActivity = WeakReference(parentActivity)

    init {
        updateUpdateTimeText(parentActivity!!)
        parentActivity.lifecycle.addObserver(this)
        if (tryToLaunchCountdownService)
            AsyncInitTask(this).execute()
    }

    private inline val loadingSnackbar
        get() = SnackbarController.ShowRequest("Ładowanie...", -1)
    private inline val nothingNewSnackbar
        get() = SnackbarController.ShowRequest("Nic nowego", 1500)
    private inline val somethingNewSnackbar
        get() = SnackbarController.ShowRequest("Zaktualizowano", 3000)
    private inline val errorSnackbar
        get() = SnackbarController.ShowRequest("Wystąpił błąd", 3000)

    //private var lastStatus = UpdateWorker.STATUS_STOPPED

    private val listener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //lastStatus = intent?.getIntExtra("status", UpdateWorker.STATUS_STOPPED) ?: return
            requestTaskOnStart()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun onCreate() {
        doOnVisible()
        LocalBroadcastManager.getInstance(weakActivity.get() ?: return)
                .registerReceiver(listener,
                        IntentFilter(UpdateWorker.ACTION_REFRESH_STATE_CHANGED))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        LocalBroadcastManager.getInstance(weakActivity.get() ?: return)
                .unregisterReceiver(listener)
    }

    override fun doOnVisible() {
        val activity = weakActivity.get() ?: return
        val snackbar = activity.snackbar

        //when (lastStatus) {
        when (UpdateWorker.currentStatus) {
            UpdateWorker.STATUS_RUNNING -> snackbar.showCancelingCurrent(loadingSnackbar)
            UpdateWorker.STATUS_STOPPED -> snackbar.cancelCurrentIfIndefinite()
            UpdateWorker.STATUS_FINISHED_NOTHING_NEW -> snackbar.showCancelingCurrent(nothingNewSnackbar)
            UpdateWorker.STATUS_ERROR -> snackbar.showCancelingCurrent(errorSnackbar)
            UpdateWorker.STATUS_FINISHED_SOMETHING_NEW -> {
                snackbar.showCancelingCurrent(somethingNewSnackbar)
                activity.requestNewMainFragment()
            }
        }
        UpdateWorker.currentStatus = UpdateWorker.STATUS_STOPPED

        updateUpdateTimeText(activity)
    }

    private fun updateUpdateTimeText(activity: MainActivity) {
        val lastCheckMillis = activity.preferences.lastCheckTime
        val lastChecked = Calendar.getInstance()
                .apply { timeInMillis = lastCheckMillis }
                .get(Calendar.DAY_OF_YEAR)

        val now = Calendar.getInstance()
                .get(Calendar.DAY_OF_YEAR)


        val text = if (lastCheckMillis == 0L)
            "Inne:"
        else
            "Sprawdzono " + when (lastChecked) {
                now -> "dzisiaj o ${DateHelper.millisToStringTimeWithoutDate(lastCheckMillis)}"
                now - 1 -> "wczoraj o ${DateHelper.millisToStringTimeWithoutDate(lastCheckMillis)}"
                now - 2 -> "przedwczoraj o ${DateHelper.millisToStringTimeWithoutDate(lastCheckMillis)}"
                else -> DateHelper.millisToStringTime(lastCheckMillis)
            }

        activity.navigationView.menu.findItem(R.id.nav_last_checked).title = text
    }

    private class AsyncInitTask(
            private val parent: MainActivitySyncObserver)
        : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg params: Unit?) {
            CountdownService.startIfNeeded(WeakReference(parent.weakActivity.get()))
        }
    }
}


inline fun Menu.forEach(func: (MenuItem) -> Unit) {
    for (i in 0 until size())
        func.invoke(getItem(i))
}
