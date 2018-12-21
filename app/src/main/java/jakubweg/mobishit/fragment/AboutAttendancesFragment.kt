package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.AttendanceDao
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.model.AboutAttendancesModel
import java.lang.ref.WeakReference

class AboutAttendancesFragment
    : AttendanceBaseSheetFragment() {

    companion object {
        fun newInstance() = AboutAttendancesFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_attendances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model = ViewModelProviders.of(this)[AboutAttendancesModel::class.java]

        MobiregPreferences.get(context!!).apply {
            if (!seenAboutAttendanceFragment)
                seenAboutAttendanceFragment = true
        }

        val weakView = WeakReference(view)
        model.types.observe(this, Observer {
            it ?: return@Observer
            weakView.get()?.apply {
                findViewById<RecyclerView>(R.id.attendanceTypeList)?.also { view ->
                    view.postDelayed({
                        view.adapter = AttendanceTypesAdapter(view.context!!, it)
                    }, 50)
                }
            }
        })

        view.findViewById<View?>(R.id.btnExcludeSubjects)?.setOnClickListener {
            val context = it.context ?: return@setOnClickListener
            val cursor = AppDatabase.getAppDatabase(context)
                    .query("SELECT Subjects.id as \"_id\", Subjects.name as name, isExcludedFromStats FROM Events \n" +
                            "INNER JOIN EventTypes ON EventTypes.id = eventTypeId\n" +
                            "LEFT OUTER JOIN Subjects ON Subjects.id = IFNULL(subjectId, 0)\n" +
                            "GROUP BY subjectId\n" +
                            "ORDER BY Subjects.name", arrayOf())

            val affectedSubjects = SparseBooleanArray()

            AlertDialog.Builder(context)
                    .setTitle("Wybierz przedmioty do wykluczenia ze statystyk")
                    .setMultiChoiceItems(cursor, "isExcludedFromStats", "name"
                    ) { _, which, isChecked ->
                        cursor.moveToPosition(which)
                        affectedSubjects.put(cursor.getInt(0), isChecked)
                    }
                    .setNegativeButton("Anuluj", null)
                    .setPositiveButton("Zapisz") { _, _ ->
                        model.updateExcludingSubjects(affectedSubjects)
                    }
                    .show()

        }
    }

    private class AttendanceTypesAdapter(context: Context,
                                         val types: MutableList<AboutAttendancesModel.TypeInfoAboutItemParent>) :
            RecyclerView.Adapter<AttendanceTypesAdapter.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        override fun getItemCount() = types.size

        override fun getItemViewType(position: Int): Int {
            return if (types[position] is AboutAttendancesModel.TypeInfoAboutItem)
                0 else 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
            return ViewHolder(inflater.inflate(if (type == 0)
                R.layout.list_item_attendance_type_text else
                R.layout.attendance_type_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            types[pos].also {
                if (it is AboutAttendancesModel.TypeInfoAboutItem)
                    holder.title.precomputedText = it.name
                else if (it is AttendanceDao.TypeInfo) {
                    holder.title.precomputedText = it.name
                    holder.colorView?.setBackgroundColor(it.color)
                }

            }
        }

        private class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.findViewById<TextView>(R.id.title)!!
            val colorView = v.findViewById<View?>(R.id.colorView)
        }
    }
}