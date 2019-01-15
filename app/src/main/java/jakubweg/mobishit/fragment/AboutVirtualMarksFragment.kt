package jakubweg.mobishit.fragment

import VirtualMarksFragment
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.AboutVirtualMarksModel

class AboutVirtualMarksFragment : Fragment() {
    companion object {
        fun newInstance() = AboutVirtualMarksFragment()

        const val STATE_NO_MARKS_SAVED = 0
        const val STATE_HAVING_POINTS_MARKS = 1
        const val STATE_HAVING_SCALE_MARKS = 2
    }

    private val viewModel get() = ViewModelProviders.of(this)[AboutVirtualMarksModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (MobiregPreferences.get(context).savedVirtualMarksState != STATE_NO_MARKS_SAVED)
                createDetailsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_virtual_marks, container, false)
    }

    private fun onMethodSelected(view: View) {
        when (view.id) {
            R.id.cardImport -> viewModel.requestSubjects()
            R.id.cardMarkScales -> viewModel.requestMarkScaleGroups()
            R.id.cardPoints -> onSelectedPointsMethod()
        }
    }

    private fun <T> askUsingDialog(items: List<T>, title: String,
                                   allowSkippingWhenOneElement: Boolean,
                                   listener: SimpleCallback<T>) {
        if (allowSkippingWhenOneElement && items.size == 1) {
            listener.call(items.first())
            return
        }
        AlertDialog.Builder(context ?: return)
                .setTitle(title)
                .setItems(Array(items.size) { items[it].toString() }) { d, pos ->
                    listener.call(items[pos])
                    Handler(Looper.getMainLooper()).postDelayed({ d.dismiss() }, 100)
                }
                .setNegativeButton("Anuluj") { di, _ -> di.cancel() }
                .setOnCancelListener {
                    viewModel.requestNothing()
                }
                .show()
    }

    private fun setLeftDrawable(textView: TextView, id: Int, color: Int) {
        textView.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context!!, id)!!.apply {
                    mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }, null, null, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        view.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 400 })

        val textColor = context.themeAttributeToColor(android.R.attr.textColorPrimary)

        prepareCardView(R.id.cardImport, R.id.textImport, R.drawable.ic_import, textColor)
        prepareCardView(R.id.cardMarkScales, R.id.textMarkScales, R.drawable.nav_marks, textColor)
        prepareCardView(R.id.cardPoints, R.id.textPoints, R.drawable.ic_more, textColor)

        viewModel.currentRealizedRequest.observe(this, observer)
    }

    private val observer = Observer<Int> {
        when (it ?: return@Observer) {
            AboutVirtualMarksModel.REQUEST_GET_MARK_SCALE_GROUPS -> onMarkScaleGroupsLoaded()
            AboutVirtualMarksModel.REQUEST_GET_SUBJECTS -> onSubjectsToImportLoaded()
            AboutVirtualMarksModel.REQUEST_GET_TERMS -> onTermsToImportLoaded()
            AboutVirtualMarksModel.REQUEST_GET_MARK_SCALE_GROUPS_BY_SUBJECT -> onMarkScaleGroupsBySubjectLoaded()
            AboutVirtualMarksModel.REQUEST_IMPORT_MARKS -> onMarksImported()
        }
    }

    private fun prepareCardView(viewId: Int, cardIconViewId: Int, iconId: Int, color: Int) {
        view!!.findViewById<CardView>(viewId)?.also { card ->
            setLeftDrawable(card.findViewById(cardIconViewId)!!, iconId, color)

            card.foreground = card.context!!.themeAttributeToDrawable(android.R.attr.selectableItemBackground)

            card.setOnClickListener(::onMethodSelected)
        }
    }


    private fun createDetailsFragment() {
        (activity as DoublePanelActivity?)?.applyNewDetailsFragment(
                VirtualMarksFragment.newInstance())
    }

    private fun onSelectedPointsMethod() {
        MobiregPreferences.get(null).markHavingPointsMarks()
        viewModel.requestNothing()
        createDetailsFragment()
    }

    private fun onMarkScaleGroupsLoaded() {
        askUsingDialog(viewModel.markScaleGroups,
                "Wybierz skalę ocen",
                true, makeCallback {
            MobiregPreferences.get(null).markHavingSavedMarkScaleGroupMarks(it.id)
            viewModel.requestNothing()
            createDetailsFragment()
        })
    }

    private fun onSubjectsToImportLoaded() {
        askUsingDialog(viewModel.subjects,
                "Wybierz przedmiot z którego zaimportować oceny",
                false, makeCallback {
            viewModel.selectedSubjectsId = it.id
            viewModel.requestScaleGroupsBySubject()
        })
    }

    private fun onMarkScaleGroupsBySubjectLoaded() {
        askUsingDialog(viewModel.markScaleGroups,
                "Wybierz skalę ocen, które mają zostać zaimportowane",
                true, makeCallback {
            viewModel.selectedMarkScaleGroupsId = it.id
            viewModel.requestTerms()
        })
    }

    private fun onTermsToImportLoaded() {
        askUsingDialog(viewModel.terms,
                "Wybierz semestr z któego zaimportować oceny",
                true, makeCallback {
            viewModel.selectedTermsId = it.id
            viewModel.requestImportingMarks()
        })
    }

    private fun onMarksImported() {
        viewModel.requestNothing()
        createDetailsFragment()
    }
}