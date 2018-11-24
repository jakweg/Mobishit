package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.ThemeHelper
import jakubweg.mobishit.helper.setText
import jakubweg.mobishit.model.MarkDetailsViewModel

@Suppress("NOTHING_TO_INLINE")
class MarkDetailsFragment : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(markId: Int): MarkDetailsFragment {
            return MarkDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt("markId", markId)
                }
            }
        }
    }

    override fun getTheme() = ThemeHelper.getBottomSheetTheme(context!!)

    fun showSelf(activity: MainActivity?) {
        show((activity as? DoublePanelActivity)?.supportFragmentManager, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.mark_details_fragment, container, false)


    private inline val Float.prettyMe: String
        get() = String.format("%.1f", this)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model = ViewModelProviders.of(this).get(MarkDetailsViewModel::class.java)

        val markId = arguments!!.getInt("markId", -1)

        model.init(markId)
        model.mark.observe(this, Observer { mark ->
            mark ?: return@Observer

            view.apply {

                setText(R.id.markTitle, mark.description.takeUnless { s -> s.isBlank() }
                        ?: "Kolumna bez nazwy")
                setText(R.id.markColumnValue, mark.columnName.takeUnless { s -> s.isBlank() }
                        ?: "Kulumna bez nazwy")

                if (mark.markPointsValue >= 0f && mark.markValueMax >= 0f && mark.countPointsWithoutBase != null) {
                    setText(R.id.markValue, mark.markPointsValue.prettyMe)

                    setText(R.id.markMarkText, "Zdobyte punkty")
                    setText(R.id.markMarkValue, "${mark.markPointsValue.prettyMe} pkt.")


                    setText(R.id.markWeightText, "Maksymalna suma punktów")
                    setText(R.id.markWeightValue, "${mark.markValueMax.prettyMe} pkt.")


                    setText(R.id.markCountToAverageText, "Wliczana do bazy")
                    setText(R.id.markCountToAverageValue, if (mark.countPointsWithoutBase) "Nie" else "Tak")

                } else if (mark.abbreviation != null && mark.defaultWeight != null && mark.noCountToAverage != null) {

                    setText(R.id.markValue, mark.abbreviation)

                    setText(R.id.markMarkText, "Słowna ocena")
                    setText(R.id.markMarkValue, mark.markName.takeUnless { s -> s.isNullOrBlank() }
                            ?: "Bez nazwy")


                    setText(R.id.markWeightText, "Waga")
                    setText(R.id.markWeightValue, "${mark.defaultWeight}")


                    setText(R.id.markCountToAverageText, "Wliczana do średniej")
                    setText(R.id.markCountToAverageValue, if (mark.noCountToAverage) "Nie" else "Tak")
                }

                if (mark.parentType == null) {
                    findViewById<View>(R.id.markParentTypeInfo)?.visibility = View.GONE
                    findViewById<View>(R.id.markParentTypeValue)?.visibility = View.GONE
                } else {
                    setText(R.id.markParentTypeValue, when (mark.parentType) {
                        null -> ""
                        MarkDao.PARENT_TYPE_COUNT_EVERY -> "Liczy się każda ocena"
                        MarkDao.PARENT_TYPE_COUNT_AVERAGE -> "Średnia z ocen"
                        MarkDao.PARENT_TYPE_COUNT_LAST -> "Liczy się ostatnia ocena"
                        MarkDao.PARENT_TYPE_COUNT_BEST -> "Liczy się lepsza ocena"
                        MarkDao.PARENT_TYPE_COUNT_WORSE -> "Liczy się gorsza ocena"
                        else -> "Nieznana (${mark.parentType})\nProszę zgłoś to programiście Mobishit"
                    })
                }

                setText(R.id.markTeacherValue, "${mark.teacherName} ${mark.teacherSurname}")
                setText(R.id.markGetDateValue, mark.formattedAddTime)
            }
        })
    }
}


