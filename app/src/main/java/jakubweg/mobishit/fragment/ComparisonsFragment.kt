package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.AttrRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.helper.EmptyAdapter
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.SnackbarController
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
                                    "Dlatego stworzyliśmy specjalny serwer, który użyje Twojego konta, pobierze porównania i rankingi i wyśle je Tobie.\n" +
                                    "Wszystkie dane są przesyłane bezpiecznym, szyfrowanym połączeniem")
                    .setPositiveButton("Rozumiem", null)
                    .show()
        }

        loadingLayout?.setOnRefreshListener { retryRunnable.run() }
        mainList?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        viewModel.status.observe(this, StatusObserver(this))
        viewModel.averages.observe(this, AveragesObserver(this))
    }

    private val loadingLayout get() = view?.findViewById<SwipeRefreshLayout?>(R.id.refreshLayout)
    private val mainList get() = view?.findViewById<RecyclerView?>(R.id.main_list)
    private var currentSnackBarMessage: SnackbarController.ShowRequest? = null

    private fun showSnackbar(request: SnackbarController.ShowRequest?) {
        request ?: return
        (activity as? MainActivity?)?.snackbar?.also {
            it.cancelCurrentIfIndefinite()
        }?.show(request)
    }

    private fun onStatusChanged(newStatus: Int) {
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

    private fun onNewAverages(averages: List<ComparisonsModel.SubjectInfo>) {
        mainList?.apply {
            adapter = if (averages.isEmpty())
                EmptyAdapter("Nie znaleziono żadnych porównań")
            else
                Adapter(context!!, averages)
        }
    }

    private class Adapter(context: Context,
                          private val averages: List<ComparisonsModel.SubjectInfo>
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)!!

        override fun getItemCount() = averages.size

        override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.comparison_list_item, viewGroup, false), iconColor)
        }

        private val iconColor = context.themeAttributeToColor(android.R.attr.textColorPrimary)

        private fun Context.themeAttributeToColor(@AttrRes attrColor: Int): Int {
            val outValue = TypedValue()
            val theme = this.theme
            theme.resolveAttribute(
                    attrColor, outValue, true)

            return ContextCompat.getColor(this, outValue.resourceId)
        }


        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            val item = averages[pos]
            holder.apply {
                subjectName.text = item.subjectName
                avgPerson.text = item.averageStudent

                avgClass.text = when {
                    item.positionInClass != null -> item.averageClass + "\n" + item.positionInClass
                    item.classImg != null -> item.averageClass + "\n" + item.classImg
                    else -> item.averageClass
                }

                avgSchool.text = when {
                    item.positionInSchool != null -> item.averageSchool + "\n" + item.positionInSchool
                    item.schoolImg != null -> item.averageSchool + "\n" + item.schoolImg
                    else -> item.averageSchool
                }
            }
        }


        private class ViewHolder(v: View, private val iconColor: Int) : RecyclerView.ViewHolder(v) {
            val subjectName = v.findViewById<TextView>(R.id.subject_name)!!
            val avgPerson = v.findViewById<TextView>(R.id.avg_person)!!
            val avgClass = v.findViewById<TextView>(R.id.avg_class)!!
            val avgSchool = v.findViewById<TextView>(R.id.avg_school)!!

            init {
                run {
                    setTextViewDrawableTint(avgPerson, R.drawable.ic_person)
                    setTextViewDrawableTint(avgClass, R.drawable.ic_group)
                    setTextViewDrawableTint(avgSchool, R.drawable.ic_school)
                }
            }

            private fun setTextViewDrawableTint(textView: TextView, iconId: Int) {
                textView.setCompoundDrawablesWithIntrinsicBounds(null,
                        tintDrawable(ContextCompat.getDrawable(textView.context, iconId)!!, iconColor),
                        null, null)
            }

            private fun tintDrawable(drawable: Drawable, tint: Int): Drawable {
                return DrawableCompat.wrap(drawable).apply {
                    DrawableCompat.setTint(this, tint)
                    DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_ATOP)
                }
            }
        }
    }


    private val retryRunnable = Runnable {
        loadingLayout?.isRefreshing = true
        viewModel.tryAgain()
    }

    private class StatusObserver(f: ComparisonsFragment)
        : Observer<Int> {
        val weakRef = WeakReference<ComparisonsFragment>(f)
        override fun onChanged(t: Int?) {
            weakRef.get()?.onStatusChanged(t ?: return)
        }
    }

    private class AveragesObserver(f: ComparisonsFragment)
        : Observer<List<ComparisonsModel.SubjectInfo>> {
        val weakRef = WeakReference<ComparisonsFragment>(f)
        override fun onChanged(t: List<ComparisonsModel.SubjectInfo>?) {
            weakRef.get()?.onNewAverages(t ?: return)
        }
    }
}