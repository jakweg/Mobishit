package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.helper.ThemeHelper
import jakubweg.mobishit.model.MarkDetailsViewModel

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

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.mark_details_fragment, container, false)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun View.setText(id: Int, text: CharSequence) {
        findViewById<TextView>(id)?.text = text
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline val Float.prettyMe: String
        get() {
            return String.format("%.1f", this)
        }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model = ViewModelProviders.of(this).get(MarkDetailsViewModel::class.java)

        val markId = arguments!!.getInt("markId", -1)

        model.init(markId)
        model.mark.observe(this, Observer {
            it ?: return@Observer

            view.apply {

                setText(R.id.markTitle, it.description.takeUnless { s -> s.isBlank() }
                        ?: "Kolumna bez nazwy")
                setText(R.id.markColumnValue, it.columnName.takeUnless { s -> s.isBlank() }
                        ?: "Kulumna bez nazwy")

                if (it.markPointsValue >= 0f && it.markValueMax >= 0f && it.countPointsWithoutBase != null) {
                    setText(R.id.markValue, it.markPointsValue.prettyMe)

                    setText(R.id.markMarkText, "Zdobyte punkty")
                    setText(R.id.markMarkValue, "${it.markPointsValue.prettyMe} pkt.")


                    setText(R.id.markWeightText, "Maksymalna suma punktów")
                    setText(R.id.markWeightValue, "${it.markValueMax.prettyMe} pkt.")


                    setText(R.id.markCountToAverageText, "Wliczana do bazy")
                    setText(R.id.markCountToAverageValue, if (it.countPointsWithoutBase) "Nie" else "Tak")

                } else if (it.abbreviation != null && it.defaultWeight != null && it.noCountToAverage != null) {

                    setText(R.id.markValue, it.abbreviation)

                    setText(R.id.markMarkText, "Słowna ocena")
                    setText(R.id.markMarkValue, it.markName.takeUnless { s -> s.isNullOrBlank() }
                            ?: "Bez nazwy")


                    setText(R.id.markWeightText, "Waga")
                    setText(R.id.markWeightValue, "${it.defaultWeight}")


                    setText(R.id.markCountToAverageText, "Wliczana do średniej")
                    setText(R.id.markCountToAverageValue, if (it.noCountToAverage) "Nie" else "Tak")
                }

                if (it.parentType == null) {
                    findViewById<View>(R.id.markParentTypeInfo)?.visibility = View.GONE
                    findViewById<View>(R.id.markParentTypeValue)?.visibility = View.GONE
                } else {
                    setText(R.id.markParentTypeValue, when (it.parentType) {
                        null -> ""
                        MarkDao.PARENT_TYPE_COUNT_EVERY -> "Liczy się każda ocena"
                        MarkDao.PARENT_TYPE_COUNT_AVERAGE -> "Średnia z ocen"
                        MarkDao.PARENT_TYPE_COUNT_LAST -> "Liczy się ostatnia ocena"
                        MarkDao.PARENT_TYPE_COUNT_BEST -> "Liczy się lepsza ocena"
                        MarkDao.PARENT_TYPE_COUNT_WORSE -> "Liczy się gorsza ocena"
                        else -> "Nieznana (${it.parentType})\nProszę zgłoś to programiście Mobishit"
                    })
                }

                setText(R.id.markTeacherValue, "${it.teacherName} ${it.teacherSurname}")
                setText(R.id.markGetDateValue, it.formattedAddTime)
            }
        })
    }
}


