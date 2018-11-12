package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.work.WorkInfo
import androidx.work.WorkManager
import jakubweg.mobishit.R
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.fragment.*
import jakubweg.mobishit.helper.DateHelper
import jakubweg.mobishit.helper.MobiregAdjectiveManager
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.SnackbarController
import jakubweg.mobishit.service.CountdownService
import jakubweg.mobishit.service.UpdateWorker
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : DoublePanelActivity() {

    companion object {
        const val ACTION_REFRESH_NOW = "refreshNow"

        const val ACTION_SHOW_MARK = "showMark"
        const val ACTION_SHOW_MESSAGE = "showMessage"
        const val ACTION_SHOW_TIMETABLE = "showTimetable"
        const val ACTION_SHOW_PREFERENCES = "showPreferences"
        const val ACTION_SHOW_COMPARISONS = "showComparisons"
        const val ACTION_UPDATE_PASSWORD = "upPass"
        const val ACTION_ABOUT_APP = "about"

        const val FRAGMENT_MARKS = "mk"
        const val FRAGMENT_TIMETABLE = "tt"
        const val FRAGMENT_MESSAGES = "mm"
    }

    private var isInForeground = false
    private var currentSelectedNavigationItemId = 0 //R.id.nav_marks
    private var updateWorkStatus: LiveData<List<WorkInfo>>? = null

    lateinit var snackbar: SnackbarController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var preferences: MobiregPreferences
    private lateinit var chooseDateItem: MenuItem

    override val mainFragmentContainerId: Int
        get() = R.id.fragment_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = MobiregPreferences.get(this)

        if (!preferences.isSignedIn) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        preferences.registerChangeListener(loginStateListener)

        navigationView = findViewById(R.id.nav_view)!!
        drawerLayout = findViewById(R.id.drawer_layout)!!
        toolbar = findViewById(R.id.toolbar)!!
        val weakActivity = WeakReference<MainActivity>(this)
        chooseDateItem = toolbar.menu!!
                .add(1, 1, 1, "Wybierz dzień").apply {
                    setIcon(R.drawable.event_note)
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    setOnMenuItemClickListener { _ ->
                        weakActivity.get()?.onChooseDateClicked()
                        true
                    }
                    isVisible = false
                }


        val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener)

        snackbar = SnackbarController(findViewById(R.id.snackbarLayout))

        BackgroundStartupTask(this).execute()

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(databaseChangedListener, AppDatabase.databaseUpdatedIntentFilter)

        if (savedInstanceState == null)
            onNewIntent(intent)
        else {
            currentSelectedNavigationItemId = savedInstanceState.getInt("currentSelectedNavigationItemId")
            adjustToSelectedNavItem(currentSelectedNavigationItemId)
            chooseDateItem.isVisible = currentSelectedNavigationItemId == R.id.nav_timetable
        }

        showUsersName()
    }

    private fun updateUpdateTimeText() {
        val lastCheckMillis = preferences.lastCheckTime
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

        navigationView.menu.findItem(R.id.nav_last_checked).title = text

    }

    @SuppressLint("SetTextI18n")
    private fun showUsersName() {
        val fullName = "${preferences.name} ${preferences.surname}"

        navigationView.getHeaderViewById<TextView>(R.id.userNameText).text = fullName

        navigationView.getHeaderViewById<TextView>(R.id.textMotto)
                .text = buildString(100) {
            append("Mobireg – dziennik tak ")
            append(MobiregAdjectiveManager.getRandom())
            append(" jak twoje oceny")
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun getCurrentMainFragment(): Fragment? {
        return when (currentSelectedNavigationItemId) {
            R.id.nav_marks -> SubjectListFragment.newInstance()
            R.id.nav_timetable -> TimetableFragment.newInstance()
            R.id.nav_tests -> TestsFragment.newInstance()
            R.id.nav_comparisons -> ComparisonsFragment.newInstance()
            R.id.nav_messages -> MessagesListFragment.newInstance()
            R.id.nav_about -> AboutFragment.newInstance()
            R.id.nav_settings -> GeneralPreferenceFragment.newInstance()
            else -> null
        }
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("currentSelectedNavigationItemId", currentSelectedNavigationItemId)
    }

    override fun onStart() {
        super.onStart()
        isInForeground = true
        updateUpdateTimeText()
    }

    override fun onStop() {
        super.onStop()
        isInForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterChangeListener(loginStateListener)
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(databaseChangedListener)
    }


    private fun onChooseDateClicked() {
        (supportFragmentManager
                ?.fragments
                ?.last { it is TimetableFragment }
                as? TimetableFragment?)
                ?.onChooseDateClicked()
    }

    private fun onUpdatePasswordRequested() {
        (supportFragmentManager
                ?.fragments
                ?.first { it is GeneralPreferenceFragment }
                as? GeneralPreferenceFragment?)
                ?.showUpdatePasswordDialog()
    }

    private fun onRefreshStateChanged(status: WorkInfo) {
        when (status.state) {
            WorkInfo.State.RUNNING -> showIndefiniteSnackbar("Ładowanie danych…")
            WorkInfo.State.ENQUEUED -> snackbar.cancelCurrentIfIndefinite()
            else -> Unit
        }
    }

    private fun showIndefiniteSnackbar(message: CharSequence) {
        val request = SnackbarController.ShowRequest(message, -1)
        snackbar.show(request)
    }

    private fun switchNavigationTo(newNavigationId: Int) {
        navigationView.menu?.forEachMenuItem(MenuItem.OnMenuItemClickListener { it ->
            if (it.itemId == newNavigationId) {
                currentSelectedNavigationItemId = newNavigationId
                if (!it.isChecked) {
                    adjustToSelectedNavItem(currentSelectedNavigationItemId)
                    it.isChecked = true
                    requestNewMainFragment()
                }
            } else {
                it.isChecked = false
            }
            return@OnMenuItemClickListener false
        })
    }

    private fun Menu.forEachMenuItem(func: MenuItem.OnMenuItemClickListener) {
        val size = this.size()
        for (i in 0 until size) {
            val item = getItem(i)
            func.onMenuItemClick(item)

            if (item.hasSubMenu())
                item.subMenu?.forEachMenuItem(func)
        }
    }
    private var handledDefaultIntent = false
    override fun onNewIntent(intent: Intent?) {
        setIntent(intent)
        if (intent != null) {
            val id = intent.getIntExtra("id", 0)
            when (intent.action ?: "") {
                "", Intent.ACTION_MAIN -> handleDefaultFragment()
                ACTION_SHOW_TIMETABLE -> {
                    switchNavigationTo(R.id.nav_timetable)
                }
                ACTION_SHOW_MARK -> {
                    switchNavigationTo(R.id.nav_marks)
                    MarkDetailsFragment.newInstance(id).showSelf(this)
                }
                ACTION_SHOW_COMPARISONS -> {
                    switchNavigationTo(R.id.nav_comparisons)
                }
                ACTION_SHOW_MESSAGE -> {
                    switchNavigationTo(R.id.nav_messages)
                    MessageDetailsFragment.newInstance(id).showSelf(this)
                }
                ACTION_REFRESH_NOW -> {
                    requestUpdatesNow()
                }
                ACTION_SHOW_PREFERENCES -> {
                    switchNavigationTo(R.id.nav_settings)
                }
                ACTION_UPDATE_PASSWORD -> {
                    switchNavigationTo(R.id.nav_settings)
                    navigationView.postDelayed({
                        onUpdatePasswordRequested()
                    }, 500L)
                }
                ACTION_ABOUT_APP -> {
                    switchNavigationTo(R.id.nav_about)
                }
            }
        }
        super.onNewIntent(intent)
    }

    private fun handleDefaultFragment() {
        if (handledDefaultIntent)
            return
        handledDefaultIntent = true
        switchNavigationTo(when (preferences.defaultFragment) {
            FRAGMENT_MARKS -> R.id.nav_marks
            FRAGMENT_TIMETABLE -> R.id.nav_timetable
            FRAGMENT_MESSAGES -> R.id.nav_messages
            else -> R.id.nav_marks
        })
    }

    private fun requestUpdatesNow() {
        if ((getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                        ?.activeNetworkInfo?.isConnected != true)
            snackbar.show(SnackbarController.ShowRequest("Aktualnie nie masz połączenia z internetem", 5000))
        UpdateWorker.requestUpdates(this)
    }

    private fun adjustToSelectedNavItem(itemId: Int) {
        when (itemId) {
            R.id.nav_timetable -> {
                chooseDateItem.isVisible = true
            }
            else -> {
                chooseDateItem.isVisible = false
            }

        }

        toolbar.title = when (itemId) {
            R.id.nav_marks -> "Oceny"
            R.id.nav_messages -> "Wiadomości i uwagi"
            R.id.nav_timetable -> "Plan lekcji"
            R.id.nav_comparisons -> "Porównania"
            R.id.nav_tests -> "Sprawdziany"
            R.id.nav_about -> "O aplikacji"
            R.id.nav_settings -> "Ustawienia"
            else -> return
        }
    }

    private fun handleNavigationItemSelection(itemId: Int) {
        when (itemId) {
            R.id.nav_marks, R.id.nav_messages, R.id.nav_timetable, R.id.nav_comparisons, R.id.nav_about, R.id.nav_settings, R.id.nav_tests -> {
                currentSelectedNavigationItemId = itemId
                requestNewMainFragment()
            }
            R.id.nav_force_refresh -> {
                requestUpdatesNow()
            }
        }
    }


    private val navigationItemSelectedListener = MainActivityNavigationListener(this)

    private class MainActivityNavigationListener(mainActivity: MainActivity) : NavigationView.OnNavigationItemSelectedListener {
        private val activity = WeakReference<MainActivity>(mainActivity)

        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            return activity.get()?.run {
                navigationView.menu
                        ?.forEachMenuItem(MenuItem.OnMenuItemClickListener {
                            it.isChecked = it.itemId == item.itemId
                            false
                        })

                adjustToSelectedNavItem(item.itemId)

                navigationView.postDelayed({ handleNavigationItemSelection(item.itemId) }, 500L)

                drawerLayout.closeDrawer(GravityCompat.START)
                true
            } ?: false
        }
    }


    private val loginStateListener = MainActivityLoginStateListener(this)

    private class MainActivityLoginStateListener(mainActivity: MainActivity) : SharedPreferences.OnSharedPreferenceChangeListener {
        private val activity = WeakReference<MainActivity>(mainActivity)
        private var ignoreFutureChanges = false
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (ignoreFutureChanges) return
            this.activity.get()?.apply {
                if (!preferences.isSignedIn) {
                    ignoreFutureChanges = true
                    preferences.unregisterChangeListener(this@MainActivityLoginStateListener)
                    if (isInForeground) {
                        startActivity(Intent(this, WelcomeActivity::class.java))
                        Toast.makeText(this, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                    return
                }
            }
        }
    }


    private val databaseChangedListener = MainActivityDatabaseChangedListener(this)

    private class MainActivityDatabaseChangedListener(mainActivity: MainActivity) : BroadcastReceiver() {
        private val activity = WeakReference<MainActivity>(mainActivity)
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val activity = this.activity.get() ?: return
            val snackBarMessage = if (intent.getBooleanExtra(AppDatabase.EXTRA_HAS_CHANGED, false)) {
                activity.requestNewMainFragment()
                "Zaktualizowano!"
            } else "Nic nowego"
            activity.snackbar.cancelAll()
            activity.snackbar.show(SnackbarController.ShowRequest(snackBarMessage, 2000L))
            activity.updateUpdateTimeText()
        }
    }


    private fun startObservingUpdates() {
        updateWorkStatus?.observe(this, Observer {
            onRefreshStateChanged(it?.firstOrNull() ?: return@Observer)
        })
    }


    private class BackgroundStartupTask(activity: MainActivity)
        : AsyncTask<Unit, Unit, Unit>() {
        private val mWeakActivity = WeakReference<MainActivity>(activity)

        private var updateWorkStatus: LiveData<List<WorkInfo>>? = null

        override fun doInBackground(vararg params: Unit?) {
            CountdownService.startIfNeeded(mWeakActivity.get() ?: return)
            updateWorkStatus = WorkManager.getInstance()
                    .getWorkInfosForUniqueWorkLiveData(UpdateWorker.UNIQUE_WORK_NAME)
        }

        override fun onPostExecute(result: Unit?) {
            mWeakActivity.get()?.also {
                it.updateWorkStatus = updateWorkStatus
                it.startObservingUpdates()
            }
        }
    }


    @Throws(ClassCastException::class)
    private inline fun <reified T : View> NavigationView.getHeaderViewById(id: Int): T {
        val found = findViewInHeaderById(id)
                ?: throw IllegalArgumentException("View with id $id not found in header")

        return found as T
    }

    private fun NavigationView.findViewInHeaderById(id: Int): View? {
        for (i in 0 until headerCount) {
            val v = getHeaderView(i)!!
            if (v.id == id) {
                return v
            } else {
                val found = v.findViewById<View?>(id)
                if (found != null)
                    return found
            }
        }
        return null
    }
}