package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.SwitchPreferenceCompat
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.EditText
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.activity.WelcomeActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.service.CountdownService
import jakubweg.mobishit.service.FcmServerNotifierWorker
import jakubweg.mobishit.service.UpdateWorker

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

            pref.setOnPreferenceClickListener { _ ->
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

            if (!value) {
                val context = preference.context!!

                val ending = when (prefs.sex) {
                    "M" -> "y"
                    "K" -> "a"
                    else -> "a/y"
                }

                @Suppress("DEPRECATION")
                val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    context.resources.getDrawable(R.drawable.ic_notifications_off_black, context.theme)
                else
                    context.resources.getDrawable(R.drawable.ic_notifications_off_black)

                AlertDialog.Builder(context)
                        .setIcon(icon)
                        .setCancelable(false)
                        .setTitle("Czy na pewno?")
                        .setMessage("Gdy wyłączysz tę funkcję, zostaniesz powiadomion$ending tylko raz na kilka godzin.\n" +
                                "Czy jesteś pewn$ending takiej decyzji?")
                        .setPositiveButton("Nie, chcę powiadomienia!") { d, _ ->
                            d.dismiss()
                            (preference as SwitchPreferenceCompat?)?.isChecked = true
                            prefs.allowedInstantNotifications = true
                        }
                        .setNegativeButton("Tak, jestem pewn$ending", null)
                        .show()
            }
            FcmServerNotifierWorker.requestPeriodicServerNotifications()
            true
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

        findPreference("key_random_device_id")?.setOnPreferenceClickListener {
            val deviceId = prefs.setRandomizedDeviceId()
            Toast.makeText(context
                    ?: return@setOnPreferenceClickListener true, "Wylosowano $deviceId", Toast.LENGTH_SHORT).show()
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

        var clicks = 0
        findPreference("refreshWeekends")?.setOnPreferenceClickListener {
            clicks++
            if (clicks >= 8) {
                val activity = this.activity ?: return@setOnPreferenceClickListener true
                if (prefs.isDeveloper)
                    Toast.makeText(activity, "Już jesteś programistą", Toast.LENGTH_SHORT).show()
                else {
                    prefs.becomeDeveloper()
                    Toast.makeText(activity, "Zostałeś programistą", Toast.LENGTH_SHORT).show()
                    activity.finish()
                    activity.startActivity(activity.intent.apply {
                        action = MainActivity.ACTION_SHOW_PREFERENCES
                    })
                }
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


        findPreference("key_clear_averages")?.setOnPreferenceClickListener { _ ->
            context?.apply {
                MobiregPreferences.get(this).hasReadyAverageCache = false
                AppDatabase.getAppDatabase(this)
                        .markDao.clearAverageCache()
            }
            false
        }

        findPreference("key_open_welcome_screen")?.setOnPreferenceClickListener { _ ->
            val context = this.context ?: return@setOnPreferenceClickListener false
            prefs.seenWelcomeActivity = false
            startActivity(Intent(context, WelcomeActivity::class.java).also {
                it.putExtra("isPreview", true)
            })
            false
        }



        findPreference("key_debug_options")?.isVisible = prefs.isDeveloper

        findPreference("key_show_fcm_token")?.setOnPreferenceClickListener {
            val token = prefs.firebaseToken
            Log.i("TOKEN", "FCM TOKEN: [$token]")
            AlertDialog.Builder(it.context ?: return@setOnPreferenceClickListener false)
                    .setMessage("Ostatni znany token FCM:\n${token ?: "nie istnieje:\\"}")
                    .setPositiveButton("OK") { d, _ -> d.dismiss() }
                    .setNegativeButton("Kopiuj do schowka") { d, _ ->
                        d.dismiss()
                        (it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)
                                ?.primaryClip = ClipData.newPlainText("FCM token", token ?: "null")
                    }
                    .show()
            false
        }
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