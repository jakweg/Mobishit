package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.fragment.*
import jakubweg.mobishit.helper.MobiregAdjectiveManager
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.SnackbarController
import jakubweg.mobishit.service.MessageUploadWorker
import jakubweg.mobishit.service.UpdateWorker

class MainActivity : DoublePanelActivity() {

    companion object {
        const val ACTION_REFRESH_NOW = "refreshNow"

        const val ACTION_SHOW_MARK = "showMark"
        const val ACTION_SHOW_MESSAGE = "showMessage"
        const val ACTION_SHOW_TIMETABLE = "showTimetable"
        const val ACTION_SHOW_PREFERENCES = "showPreferences"
        const val ACTION_SHOW_COMPARISONS = "showComparisons"
        const val ACTION_SHOW_ATTENDANCE_STATS = "showAttendances"
        const val ACTION_UPDATE_PASSWORD = "upPass"
        const val ACTION_ABOUT_APP = "about"

        const val FRAGMENT_MARKS = "mk"
        const val FRAGMENT_TIMETABLE = "tt"
        const val FRAGMENT_MESSAGES = "mm"

        private var isInForeground = false
        val isMainActivityInForeground get() = isInForeground
    }

    val preferences get() = MobiregPreferences.get(this)

    override val mainFragmentContainerId: Int
        get() = R.id.fragment_container

    val navigationView get() = findViewById<NavigationView?>(R.id.nav_view)!!
    val toolbar get() = findViewById<Toolbar>(R.id.toolbar)!!
    val drawerLayout get() = findViewById<DrawerLayout>(R.id.drawer_layout)!!

    lateinit var snackbar: SnackbarController
    private lateinit var navigationUtils: MainActivityNavigationLayoutUtils
    private var currentSelectedItemId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!preferences.isSignedIn) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        showUsersName()

        snackbar = SnackbarController(findViewById(R.id.snackbarLayout))
        currentSelectedItemId = savedInstanceState?.getInt("currentSelectedItemId") ?: 0

        MainActivityLoginListener(this)
        navigationUtils = MainActivityNavigationLayoutUtils(this)
        MainActivitySyncObserver(this,
                preferences.runCountdownService && savedInstanceState == null)

        handleIntent(intent, savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    private fun showUsersName() {
        val fullName = "${preferences.name} ${preferences.surname}"

        navigationView.getHeaderViewById<TextView>(R.id.userNameText).text = fullName

        navigationView.getHeaderViewById<TextView>(R.id.textMotto)
                .text = "Mobireg – dziennik tak ${
        if (preferences.sex == "M")
            MobiregAdjectiveManager.getRandom()
        else "świetny"} jak twoje oceny"
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent ?: return, null)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle?) {
        navigationUtils.handleIntent(this, intent, savedInstanceState)
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("currentSelectedItemId", currentSelectedItemId)
    }

    fun onNavigationItemSelected(itemId: Int, requestNewLayout: Boolean) {
        when (itemId) {
            R.id.nav_force_refresh ->
                MessageUploadWorker.requestMessageSent(
                        this,
                        0,
                        "chuj",
                        "chujchuj")
            //tryToRefresh()

            R.id.nav_app_update ->
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(preferences.getAppUpdateInfo()
                                ?.urlDoDownload ?: return)))
            else -> {
                currentSelectedItemId = itemId
                if (requestNewLayout)
                    requestNewMainFragment()
            }
        }
    }

    override fun getCurrentMainFragment(): Fragment? {
        return when (currentSelectedItemId) {
            R.id.nav_marks -> SubjectListFragment.newInstance()
            R.id.nav_timetable -> TimetableFragment.newInstance()
            R.id.nav_tests -> TestsFragment.newInstance()
            R.id.nav_attendances -> AttendancesSummaryFragment.newInstance()
            R.id.nav_comparisons -> ComparisonsFragment.newInstance()
            R.id.nav_messages -> MessagesListFragment.newInstance()
            R.id.nav_about -> AboutFragment.newInstance()
            R.id.nav_settings -> GeneralPreferenceFragment.newInstance()
            R.id.nav_calculate_average -> VirtualMarksFragment.newInstance()
            else -> null
        }
    }

    inline fun <reified T> getLastFragment(): T? {
        supportFragmentManager.fragments
                .reversed()
                .forEach {
                    if (it is T)
                        return it
                }
        return null
    }

    fun onToolbarItemClicked(itemId: Int) {
        when (itemId) {
            R.id.nav_choose_date ->
                getLastFragment<TimetableFragment>()
                        ?.onChooseDateClicked()
            R.id.nav_about_attendances ->
                AboutAttendancesFragment
                        .newInstance()
                        .showSelf(this)
            R.id.nav_sort_by ->
                MarksViewOptionsFragment
                        .newInstance(getLastFragment<SubjectsMarkFragment>() != null)
                        .showSelf(this)
        }
    }

    fun tryToRefresh() {
        if ((getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                        ?.activeNetworkInfo?.isConnected != true)
            snackbar.show(SnackbarController.ShowRequest("Aktualnie nie masz połączenia z internetem", 5000))
        UpdateWorker.requestUpdates(this)
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