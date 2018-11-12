package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.TestData
import jakubweg.mobishit.helper.EmptyAdapter
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.SnackbarController
import jakubweg.mobishit.model.TestsModel
import java.lang.ref.WeakReference


class TestsFragment : Fragment() {
    companion object {
        fun newInstance() = TestsFragment()
    }

    private val model by
    lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this)[TestsModel::class.java]
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(
                (if (MobiregPreferences.get(context ?: return null).allowedInstantNotifications)
                    R.layout.fragment_tests
                else R.layout.fragment_tests_no_permission), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btnAllowServer)?.setOnClickListener {
            (activity as MainActivity?)?.apply {
                snackbar.show(SnackbarController.ShowRequest("✔️ Nadano pozwolenie", 1000L))
                MobiregPreferences.get(this).apply {
                    allowedInstantNotifications = true
                }
                requestNewMainFragment()
            }
        }
        view.findViewById<View>(R.id.btnInfo)?.setOnClickListener {
            AlertDialog.Builder(context ?: return@setOnClickListener)
                    .setTitle("Jak to działa?")
                    .setMessage(
                            "Mobireg nie wysyła sprawdzianów podczas synchronizacji, dlatego nie możemy i od tak wyświetlić.\n" +
                                    "Stworzyliśmy jednak specjalny serwer, który zaloguje się na Twoje konto, pobierze sprawdziany i wyśle je Tobie.\n" +
                                    "Wszystkie dane są przesyłane bezpiecznym, szyfrowanym połączeniem.")
                    .setPositiveButton("Rozumiem", null)
                    .show()
        }
        loadingLayout?.setOnRefreshListener {
            startRefreshingRunnable.run()
            model.refreshDataFromInternet()
        }

        val mgr = LinearLayoutManager(context!!)
        testsList?.layoutManager = mgr
        testsList?.addItemDecoration(DividerItemDecoration(context!!, mgr.orientation))

        model.tests.observe(this, TestsObserver(this))
        model.status.observe(this, StatusObserver(this))
    }

    private val loadingLayout get() = view?.findViewById<SwipeRefreshLayout?>(R.id.refreshLayout)
    private val testsList get() = view?.findViewById<RecyclerView?>(R.id.testsList)

    private fun onNewStatus(newStatus: Int) {
        if (newStatus == TestsModel.STATUS_DOWNLOADING) {
            loadingLayout?.isRefreshing = true
        } else {
            val (askForRetry, text) = when (newStatus) {
                TestsModel.STATUS_DONE -> false to "Wczytano sprawdziany"
                TestsModel.STATUS_ERROR_CONNECTION_ERROR -> false to "Brak połączenia z internetem"
                TestsModel.STATUS_ERROR_UNKNOWN -> true to "Wystąpił nieznany błąd"
                TestsModel.STATUS_ERROR_SERVER_ERROR -> true to "Wystąpił błąd serwerowy"
                TestsModel.STATUS_ERROR_MALFORMED_RESPONSE -> false to "Serwer zwrócił dziwną odpowiedź"
                TestsModel.STATUS_ERROR_NOT_ALLOWED -> false to "Brak uprawnień do synchronizacji"
                TestsModel.STATUS_UNKNOWN -> {
                    loadingLayout?.postDelayed({ model.considerRefreshingData() }, 500L)
                    return
                }
                else -> throw IllegalArgumentException()
            }

            val request = if (askForRetry)
                SnackbarController.ShowRequest(text, "Ponów próbę", 3000,
                        SnackbarController.WeakClickedListener(startRefreshingRunnable))
            else
                SnackbarController.ShowRequest(text, 1800)

            (activity as? MainActivity)?.snackbar?.show(request)
            loadingLayout?.isRefreshing = false
        }
    }

    private fun onTestsChanged(newTests: List<TestData>) {
        testsList?.adapter =
                if (newTests.isEmpty()) EmptyAdapter("Nic tu nie ma")
                else TestsAdapter(activity as MainActivity, newTests, model.firstInPastIndex)
    }

    private val startRefreshingRunnable = Runnable {
        if (loadingLayout?.isRefreshing != false)
            return@Runnable
        loadingLayout?.isRefreshing = true
        model.refreshDataFromInternet()
    }

    private class TestsObserver(f: TestsFragment?) : Observer<List<TestData>> {
        private val fragment = WeakReference<TestsFragment>(f)
        override fun onChanged(newStatus: List<TestData>?) {
            fragment.get()?.onTestsChanged(newStatus ?: emptyList())
        }
    }

    private class StatusObserver(f: TestsFragment?) : Observer<Int> {
        private val fragment = WeakReference<TestsFragment>(f)
        override fun onChanged(newStatus: Int?) {
            fragment.get()?.onNewStatus(newStatus ?: TestsModel.STATUS_UNKNOWN)
        }
    }

    private class TestsAdapter(
            activity: MainActivity,
            private val newTests: List<TestData>,
            private val firstInPastIndex: Int)
        : RecyclerView.Adapter<TestsAdapter.ViewHolder>() {
        companion object {
            private const val TYPE_TEST_INFO = 0
            private const val TYPE_TEXT_ABOUT_PAST = 1
        }

        override fun getItemCount(): Int {
            return if (firstInPastIndex != -1)
                newTests.size + 1
            else
                newTests.size
        }

        private val inflater = LayoutInflater.from(activity)!!

        @SuppressLint("SetTextI18n")
        override fun onCreateViewHolder(group: ViewGroup, type: Int): ViewHolder {
            return when (type) {
                TYPE_TEST_INFO -> ViewHolder(inflater.inflate(R.layout.list_item_test_info, group, false))
                TYPE_TEXT_ABOUT_PAST -> ViewHolder(inflater.inflate(R.layout.text_about_tests_in_past, group, false))
                else -> throw IllegalArgumentException()
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == firstInPastIndex)
                TYPE_TEXT_ABOUT_PAST
            else
                TYPE_TEST_INFO
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            if (pos == firstInPastIndex)
                return //this is text

            val item = newTests[if (firstInPastIndex in 0..pos) pos - 1 else pos]

            if (item.isInPast)
                holder.itemView.alpha = 0.6f
            else if (holder.itemView.alpha != 1f)
                holder.itemView.alpha = 1f

            holder.date!!.text = item.date
            holder.title!!.text = item.description
            holder.secondaryInfo!!.text = "${item.subject} \u2022 ${item.teacher}"
        }

        private class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val date = v.findViewById<TextView>(R.id.testDate) ?: null
            val title = v.findViewById<TextView>(R.id.testTitle) ?: null
            val secondaryInfo = v.findViewById<TextView>(R.id.testSecondary) ?: null
        }
    }
}