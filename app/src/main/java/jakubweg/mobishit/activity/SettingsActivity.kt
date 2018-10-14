package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.EditText
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.service.CountdownService
import jakubweg.mobishit.service.UpdateWorker


class SettingsActivity : DoublePanelActivity() {

    companion object {
        const val ACTION_UPDATE_PASSWORD = "upPass"
    }

    override val mainFragmentContainerId: Int
        get() = R.id.fragment_container

    override fun getCurrentMainFragment() = GeneralPreferenceFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        if (savedInstanceState == null &&
                intent?.action == ACTION_UPDATE_PASSWORD)
            showUpdatePasswordDialog()

    }

    private fun showUpdatePasswordDialog() {
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
                }
                .create().show()
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager!!.sharedPreferencesName = "mobireg"
            addPreferencesFromResource(R.xml.pref_general)

            val prefs = MobiregPreferences.get(context!!)

            findPreference("key_log_out")?.also { pref ->
                pref.isEnabled = prefs.isSignedIn

                pref.setOnPreferenceClickListener { _ ->
                    activity?.apply {
                        prefs.logout(this)
                        pref.isEnabled = false
                    }
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

            findPreference("key_open_github")?.setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JakubekWeg/Mobishit")))
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
                        activity.startActivity(activity.intent)
                    }
                }
                true
            }

            findPreference("theme")?.setOnPreferenceChangeListener { _, newTheme ->
                if (newTheme == null || newTheme !is String) return@setOnPreferenceChangeListener false

                activity?.also { it.finish(); it.startActivity(it.intent) }

                true
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
}
