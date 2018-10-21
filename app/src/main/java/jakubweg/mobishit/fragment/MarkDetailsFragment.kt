package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Build
import android.os.Bundle
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.db.MarkDao
import jakubweg.mobishit.model.MarkDetailsViewModel

class MarkDetailsFragment : Fragment() {
    companion object {
        fun newInstance(markId: Int) = newInstance(markId, null, null, null, null)

        fun newInstance(markId: Int,
                        description: String?,
                        descriptionTransitionName: String?,
                        subjectName: String?,
                        subjectTransitionName: String?) = MarkDetailsFragment().apply {
            arguments = Bundle().also {
                it.putInt("markId", markId)
                it.putString("description", description)
                it.putString("descriptionTransitionName", descriptionTransitionName)
                it.putString("subjectName", subjectName)
                it.putString("subjectTransitionName", subjectTransitionName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
            sharedElementReturnTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.mark_details_fragment, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model = ViewModelProviders.of(this).get(MarkDetailsViewModel::class.java)

        arguments!!.apply {
            view.findViewById<TextView>(R.id.textSubjectName).also {
                ViewCompat.setTransitionName(it, getString("subjectTransitionName") ?: "")
                it.text = getString("subjectName") ?: ""
            }
            view.findViewById<TextView>(R.id.textMarkName).also {
                ViewCompat.setTransitionName(it, getString("descriptionTransitionName") ?: "")
                it.text = getString("description") ?: ""
            }
        }

        val markId = arguments!!.getInt("markId", -1)

        model.init(markId)
        model.mark.observe(this, Observer {
            it ?: return@Observer

            view.apply {
                findViewById<View>(R.id.loadingCircle).visibility = View.GONE
                findViewById<View>(R.id.contentLayout).visibility = View.VISIBLE


                val hasPointsValues = it.markPointsValue != null && it.markValueMax != null && it.countPointsWithoutBase != null
                val hasScaleMarkInfo = it.abbreviation != null && it.defaultWeight != null && it.noCountToAverage != null

                if (!hasPointsValues) {
                    findViewById<View>(R.id.markPointsLayout).visibility = View.GONE
                    findViewById<View>(R.id.markNoPointsLayout).visibility = View.VISIBLE
                }


                if (!hasScaleMarkInfo) {
                    findViewById<View>(R.id.markPointsLayout).visibility = View.VISIBLE
                    findViewById<View>(R.id.markNoPointsLayout).visibility = View.GONE
                }

                findViewById<TextView>(R.id.textSubjectName).text = it.subjectName
                findViewById<TextView>(R.id.textMarkName).text = it.description
                findViewById<TextView>(R.id.textMarkColumn).text = it.columnName


                findViewById<TextView>(R.id.textMarkScaleValue).text = "${it.markName} (${it.abbreviation})"
                findViewById<TextView>(R.id.textMarkWeight).text = String.format("%.1f", it.defaultWeight
                        ?: 0f)
                findViewById<TextView>(R.id.textMarkIsCountToAverage).text = if (it.noCountToAverage == false) "Tak" else "Nie"



                findViewById<TextView>(R.id.textMarkPointsValue).text = String.format("%.1f pkt.", it.markPointsValue
                        ?: 0f)
                findViewById<TextView>(R.id.textMarkMaxPoints).text = String.format("%.1f pkt.", it.markValueMax
                        ?: 0f)
                findViewById<TextView>(R.id.textMarkIsCountToBase).text = if (it.countPointsWithoutBase == false) "Tak" else "Nie"

                val parentTypeVisibility = if (it.parentType == null) View.GONE else View.VISIBLE
                findViewById<View>(R.id.layoutParentType).visibility = parentTypeVisibility
                findViewById<View>(R.id.separatorParentType).visibility = parentTypeVisibility

                findViewById<TextView>(R.id.textParentType)
                        .text = when (it.parentType) {
                    null -> ""
                    MarkDao.PARENT_TYPE_COUNT_AVERAGE -> "Średnia z obu ocen"
                    MarkDao.PARENT_TYPE_COUNT_BEST -> "Liczy się lepsza ocena"
                    MarkDao.PARENT_TYPE_COUNT_LAST -> "Liczy się ostatnia ocena"
                    else -> "Nieznana (${it.parentType})\nProszę zgłoś to programiście Mobishit"
                }

                findViewById<TextView>(R.id.textMarkGetDate).text = it.formattedGetDate
                findViewById<TextView>(R.id.textMarkAddTime).text = it.formattedAddTime
                findViewById<TextView>(R.id.textMarkTeacherName).text = "${it.teacherName} ${it.teacherSurname}"
            }
        })
    }
}


