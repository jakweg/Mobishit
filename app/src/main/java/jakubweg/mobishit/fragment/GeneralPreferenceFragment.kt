package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.SwitchPreferenceCompat
import android.text.SpannableStringBuilder
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.EditText
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.SimpleCallback2
import jakubweg.mobishit.helper.TimetableWidgetProvider
import jakubweg.mobishit.helper.makeCallback2
import jakubweg.mobishit.service.CountdownService
import jakubweg.mobishit.service.FcmServerNotifierWorker
import jakubweg.mobishit.service.UpdateWorker
import java.lang.ref.WeakReference

class GeneralPreferenceFragment : PreferenceFragmentCompat() {
    companion object {
        fun newInstance() = GeneralPreferenceFragment()
    }

    fun showUpdatePasswordDialog() {
        context?.apply {
            val editText = EditText(this).apply {
                layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
                hint = "Twoje nowe hasło"
                inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }

            AlertDialog.Builder(this)
                    .setTitle("Aktualizacja hasła:")
                    .setView(editText)
                    .setNegativeButton("Anuluj") { dialog, _ -> dialog.cancel() }
                    .setPositiveButton("Zapisz") { dialog, _ ->
                        dialog.dismiss()
                        MobiregPreferences.get(this).setPassword(editText.text.toString())
                        UpdateWorker.requestUpdates(this)
                        FcmServerNotifierWorker.requestPeriodicServerNotifications()
                    }
                    .create().show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager!!.sharedPreferencesName = "mobireg"
        addPreferencesFromResource(R.xml.pref_general)

        val prefs = MobiregPreferences.get(context!!)

        findPreference("key_log_out")?.also { pref ->

            pref.setOnPreferenceClickListener {
                val context = this.context ?: return@setOnPreferenceClickListener true
                AlertDialog.Builder(context)
                        .setMessage("Czy na pewno chcesz się wylogować?")
                        .setPositiveButton("Tak") { _, _ ->
                            prefs.logout(context)
                            pref.isEnabled = false
                        }
                        .setNegativeButton("nie", null)
                        .show()
                true
            }
        }

        findPreference("allowIN")?.setOnPreferenceChangeListener { preference, value ->
            if (value !is Boolean)
                return@setOnPreferenceChangeListener false

            return@setOnPreferenceChangeListener if (!value) {
                val context = preference.context!!

                val ending = when (prefs.sex) {
                    "M" -> "y"
                    "K" -> "a"
                    else -> "a/y"
                }

                val icon = AppCompatResources.getDrawable(context, R.drawable.ic_notifications_off_black)

                AlertDialog.Builder(context)
                        .setIcon(icon)
                        .setCancelable(false)
                        .setTitle("Czy na pewno?")
                        .setMessage("Gdy wyłączysz tę funkcję, zostaniesz powiadomion$ending tylko raz na kilka godzin.\n" +
                                "Czy jesteś pewn$ending takiej decyzji?")
                        .setPositiveButton("Nie, chcę powiadomienia!") { _, _ ->
                            (preference as SwitchPreferenceCompat?)?.isChecked = true
                            prefs.allowedInstantNotifications = true
                        }
                        .setNegativeButton("Tak, jestem pewn$ending") { _, _ ->
                            (preference as SwitchPreferenceCompat?)?.isChecked = false
                            prefs.allowedInstantNotifications = true
                            FcmServerNotifierWorker.requestPeriodicServerNotifications()
                        }
                        .show()
                false
            } else {
                FcmServerNotifierWorker.requestPeriodicServerNotifications()
                true
            }
        }


        findPreference("refreshFrequency")?.setOnPreferenceChangeListener { _, value ->
            if (value is String) {
                prefs.setRefreshFrequency(value.toInt())
                UpdateWorker.requestUpdates(context
                        ?: return@setOnPreferenceChangeListener true)
            }
            true
        }


        (findPreference("beforeLessonsMinutes") as? EditTextPreference?)
                ?.setOnPreferenceChangeListener { preference, value ->
                    if (value !is String) return@setOnPreferenceChangeListener false
                    val minutes = value.toIntOrNull()
                    if (minutes == null) {
                        Toast.makeText(preference.context!!, "Nieprawidłowa liczba", Toast.LENGTH_SHORT).show()
                        return@setOnPreferenceChangeListener false
                    }
                    if (minutes !in 0..120) {
                        Toast.makeText(preference.context!!, "Zbyt duża lub mała liczba", Toast.LENGTH_SHORT).show()
                        return@setOnPreferenceChangeListener false
                    }

                    // yeah, on main thread :/
                    CountdownService.stop(context ?: return@setOnPreferenceChangeListener true)
                    CountdownService.startIfNeeded(WeakReference(context))

                    true
                }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            findPreference("notifyWithSound")?.isVisible = false
        }


        findPreference("key_open_notification_settings")?.setOnPreferenceClickListener {
            startActivity(openNotificationSettingsIntent
                    ?: return@setOnPreferenceClickListener true)
            true
        }

        findPreference("runCountdownService")?.setOnPreferenceChangeListener { _, value ->
            if (value !is Boolean) return@setOnPreferenceChangeListener false
            try {
                val context = requireContext()
                Handler(Looper.getMainLooper()).postDelayed({
                    if (value) {
                        prefs.nextAllowedCountdownServiceStart = 0L
                        CountdownService.start(context)
                    } else {
                        CountdownService.stop(context)
                    }
                }, 100)
            } catch (e: Exception) {
                Toast.makeText(context!!, "Can't run the service", Toast.LENGTH_SHORT).show()
            }
            true
        }

        findPreference("theme")?.setOnPreferenceChangeListener { _, newTheme ->
            if (newTheme == null || newTheme !is String) return@setOnPreferenceChangeListener false

            activity?.also { activity ->
                activity.finish()
                activity.startActivity(Intent(activity, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_SHOW_PREFERENCES
                })
            }

            true
        }

        (findPreference("sn") as? ListPreference)?.setOnPreferenceClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                TimetableWidgetProvider.requestInstantUpdate(it?.context ?: return@postDelayed)
            }, 1000L)
            true
        }


        findPreference("key_change_subjects_name")?.setOnPreferenceClickListener {
            showRenameDialog(it.context ?: return@setOnPreferenceClickListener false,
                    "name", "Subjects", "Wybierz przedmiot", "Zmień nazwę przedmotu na", "Nazwa przedmiotu",
                    makeCallback2 { id, name ->
                        AppDatabase.getAppDatabase(it.context!!)
                                .compileStatement("UPDATE Subjects SET name = ? WHERE id = ?").apply {
                                    bindString(1, name.trim())
                                    bindLong(2, id.toLong())
                                }.executeUpdateDelete()
                    })
            false
        }

        findPreference("key_change_teacher_name")?.setOnPreferenceClickListener {
            val context = it.context ?: return@setOnPreferenceClickListener false
            showRenameDialog(context, "name || ' ' || surname", "Teachers",
                    "Wybierz nauczyciela", "Daj mu ksywkę", "ksywka",
                    makeCallback2 { id, name ->
                        AppDatabase.getAppDatabase(it.context!!)
                                .compileStatement("UPDATE Teachers SET name = ?, surname = ? WHERE id = ?").apply {
                                    val spacePos = name.indexOf(' ')
                                    if (spacePos >= 0) {
                                        bindString(1, name.substring(0, spacePos).trim())
                                        bindString(2, name.substring(spacePos).trim())
                                    } else {
                                        bindString(1, name)
                                        bindString(2, "")
                                    }
                                    bindLong(3, id.toLong())
                                }.executeUpdateDelete()
                    })

            false
        }

        findPreference("key_attendance_policy")?.setOnPreferenceClickListener {

            val options = arrayOf(
                    "Nowe obecności",
                    "Nowe nieobecności i spóźnienia",
                    "Zmiany we frekwencji")

            val policy = prefs.attendanceNotificationPolicy
            val checked = booleanArrayOf(
                    policy and MobiregPreferences.ATTENDANCE_NOTIFICATION_PRESENT > 0,
                    policy and MobiregPreferences.ATTENDANCE_NOTIFICATION_ABSENT > 0,
                    policy and MobiregPreferences.ATTENDANCE_NOTIFICATION_CHANGE > 0
            )

            AlertDialog.Builder(context!!)
                    .setTitle("O czym powiadamiać?")
                    .setMultiChoiceItems(options, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setOnDismissListener {
                        var newPolicy = 0
                        for (b in checked) {
                            newPolicy = newPolicy shl 1
                            if (b) newPolicy++
                        }
                        prefs.attendanceNotificationPolicy = newPolicy
                    }
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            false
        }

    }

    private fun showRenameDialog(context: Context,
                                 columnName: String,
                                 tableName: String,
                                 chooseObjectTitle: String,
                                 renameObjectTitle: String,
                                 hint: String,
                                 successCallback: SimpleCallback2<Int, String>) {
        val cursor = AppDatabase.getAppDatabase(context)
                .query("SELECT id AS _id, $columnName as _name FROM $tableName WHERE length(_name) > 1 ORDER BY 2", emptyArray())!!
        AlertDialog.Builder(context)
                .setTitle(chooseObjectTitle)
                .setCursor(cursor, { _, which ->
                    cursor.moveToPosition(which)
                    val textView = EditText(context)
                    textView.hint = hint
                    textView.text = SpannableStringBuilder(cursor.getString(cursor.getColumnIndex("_name")))

                    AlertDialog.Builder(context)
                            .setView(textView)
                            .setTitle(renameObjectTitle)
                            .setNegativeButton("Anuluj", null)
                            .setPositiveButton("Zapisz") { _, _ ->
                                val value = textView.text?.toString()?.takeUnless { it.isBlank() }
                                        ?: return@setPositiveButton
                                val subjectId = cursor.getInt(cursor.getColumnIndex("_id"))

                                successCallback.call(subjectId, value)

                                restartServices(context)
                                Toast.makeText(context, "Zapisano!", Toast.LENGTH_SHORT).show()
                            }
                            .show()
                }, "_name")
                .show()
    }

    private fun restartServices(context: Context) {
        CountdownService.stop(context)
        Handler(Looper.getMainLooper()).postDelayed({
            CountdownService.start(context)
            TimetableWidgetProvider.requestInstantUpdate(context)
            MobiregPreferences.get(context).apply {
                hasReadyAverageCache = false
                hasReadyLastMarksCache = false
            }
        }, 250L)
    }

    private val openNotificationSettingsIntent
        @SuppressLint("ObsoleteSdkInt")
        get(): Intent? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                return null

            val context = context ?: return null

            return Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                } else {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
        }
}