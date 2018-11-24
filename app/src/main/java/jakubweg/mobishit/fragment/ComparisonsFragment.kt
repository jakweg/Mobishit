package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.ComparisonCacheData
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.ComparisonsModel
import java.lang.ref.WeakReference


class ComparisonsFragment : Fragment() {
    companion object {
        fun newInstance() = ComparisonsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(
                if (MobiregPreferences.get(context).allowedInstantNotifications)
                    R.layout.fragment_comparisons
                else
                    R.layout.fragment_comparisons_no_permission
                , container, false)
    }


    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this)[ComparisonsModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = this.context!!

        view.textView(R.id.avg_person)?.setLeftDrawable(R.drawable.ic_person)
        view.textView(R.id.avg_class)?.setLeftDrawable(R.drawable.ic_group)
        view.textView(R.id.avg_school)?.setLeftDrawable(R.drawable.ic_school)

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
            AlertDialog.Builder(context)
                    .setTitle("Jak to działa?")
                    .setMessage(
                            "Mobireg nie wysyła porównań bezpośrednio w API, więc nie możemy ich od tak tutaj wyświetlić.\n" +
                                    "Dlatego stworzyliśmy specjalny serwer, który użyje Twojego konta, aby pobrać porównania oraz rankingi i wysłać je Tobie.\n" +
                                    "Wszystkie dane są przesyłane bezpiecznym, szyfrowanym połączeniem.")
                    .setPositiveButton("Rozumiem", null)
                    .show()
        }

        loadingLayout?.setOnRefreshListener { retryRunnable.run() }
        mainList?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        viewModel.averages.observe(this, AveragesObserver(this))
        viewModel.status.observe(this, StatusObserver(this))

        if (viewModel.averages.value?.isNotEmpty() != true) {
            mainList?.adapter = EmptyAdapter("Brak dostępnych porównań\nPołącz się z internetem lub spróbuj ponownie później")
        }
    }

    private val loadingLayout get() = view?.findViewById<SwipeRefreshLayout?>(R.id.refreshLayout)
    private val mainList get() = view?.findViewById<RecyclerView?>(R.id.main_list)
    private var currentSnackBarMessage: SnackbarController.ShowRequest? = null

    private fun showSnackbar(request: SnackbarController.ShowRequest?) {
        request ?: return
        request.cancel()
        (activity as? MainActivity?)?.snackbar?.also {
            it.cancelCurrentIfIndefinite()
        }?.show(request)
    }


    private fun onStatusChanged(newStatus: Int) {
        if (newStatus == ComparisonsModel.STATUS_NONE) {
            loadingLayout?.postDelayed({ viewModel.considerRefreshingData() }, 500L)
            return
        }

        if (newStatus == ComparisonsModel.STATUS_DOWNLOADING) {
            loadingLayout?.isRefreshing = true
            return
        }

        loadingLayout?.isRefreshing = false
        if (newStatus == ComparisonsModel.STATUS_DONE) {
            return
        }

        if (newStatus == ComparisonsModel.STATUS_NO_INTERNET) {
            currentSnackBarMessage = SnackbarController.ShowRequest("Brak połączenia z internetem",
                    "Jeszcze raz", -1,
                    SnackbarController.WeakClickedListener(retryRunnable))
            showSnackbar(currentSnackBarMessage)
            return
        }

        val msg = when (newStatus) {
            ComparisonsModel.STATUS_CLIENT_ERROR -> "Wystąpił lokalny błąd"
            ComparisonsModel.STATUS_SERVER_ERROR -> "Wystąpił serwerowy błąd"
            else -> return
        }

        showSnackbar(SnackbarController.ShowRequest(msg, 5000L))
    }

    override fun onDestroy() {
        super.onDestroy()
        currentSnackBarMessage?.cancel()
    }

    private fun onNewAverages(averages: List<ComparisonCacheData>) {
        mainList?.apply {
            adapter = if (averages.isEmpty())
                EmptyAdapter("Nie znaleziono żadnych porównań")
            else
                Adapter(context!!, averages)
            startAnimation(AlphaAnimation(0f, 1f).also { it.duration = 300 })
        }
    }


    private class Adapter(context: Context,
                          private val averages: List<ComparisonCacheData>
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)!!

        override fun getItemCount() = averages.size

        override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.comparison_list_item, viewGroup, false), iconColor)
        }

        private val iconColor = context.themeAttributeToColor(android.R.attr.textColorPrimary)


        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            val item = averages[pos]
            holder.apply {
                subjectName.precomputedText = item.subjectName
                avgPerson.precomputedText = item.averageStudent

                avgClass.precomputedText = when {
                    item.positionInClass != null -> item.averageClass + "\n" + item.positionInClass
                    item.classImg != null -> item.averageClass + "\n" + item.classImg
                    else -> item.averageClass
                }

                avgSchool.precomputedText = when {
                    item.positionInSchool != null -> item.averageSchool + "\n" + item.positionInSchool
                    item.schoolImg != null -> item.averageSchool + "\n" + item.schoolImg
                    else -> item.averageSchool
                }
            }
        }


        private class ViewHolder(v: View, iconColor: Int) : RecyclerView.ViewHolder(v) {
            val subjectName = v.textView(R.id.subject_name)!!
            val avgPerson = v.textView(R.id.avg_person)!!
            val avgClass = v.textView(R.id.avg_class)!!
            val avgSchool = v.textView(R.id.avg_school)!!

            init {
                run {
                    avgPerson.setTopDrawable(R.drawable.ic_person, iconColor)
                    avgClass.setTopDrawable(R.drawable.ic_group, iconColor)
                    avgSchool.setTopDrawable(R.drawable.ic_school, iconColor)
                }
            }

        }
    }


    private val retryRunnable = Runnable {
        loadingLayout?.isRefreshing = true
        viewModel.refreshDataFromInternet()
    }

    private class StatusObserver(f: ComparisonsFragment)
        : Observer<Int> {
        val weakRef = WeakReference<ComparisonsFragment>(f)
        override fun onChanged(t: Int?) {
            weakRef.get()?.onStatusChanged(t ?: return)
        }
    }

    private class AveragesObserver(f: ComparisonsFragment)
        : Observer<List<ComparisonCacheData>> {
        val weakRef = WeakReference<ComparisonsFragment>(f)
        override fun onChanged(t: List<ComparisonCacheData>?) {
            weakRef.get()?.onNewAverages(t ?: return)
        }
    }
}