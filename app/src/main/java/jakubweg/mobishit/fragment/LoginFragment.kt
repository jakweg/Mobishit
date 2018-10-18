package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.SettingsActivity
import jakubweg.mobishit.activity.WelcomeActivity
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.model.LoginDataModel
import jakubweg.mobishit.service.AppUpdateWorker
import jakubweg.mobishit.service.UpdateWorker

class LoginFragment : Fragment() {
    companion object {
        fun newInstance() = LoginFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this)[LoginDataModel::class.java]
    }

    private inline val editLogin: TextView? get() = view?.findViewById(R.id.editLogin)
    private inline val editPass: TextView? get() = view?.findViewById(R.id.editPass)
    private inline val confirmBtn: View? get() = view?.findViewById(R.id.confirmBtn)
    private inline val progressBar: ProgressBar? get() = view?.findViewById(R.id.progressbar)
    private inline val noInternetText: View? get() = view?.findViewById(R.id.noInternetText)
    private inline val btnAboutPrivacy: View? get() = view?.findViewById(R.id.btnAboutPrivacy)

    private fun toast(msg: CharSequence) {
        Toast.makeText(context ?: return, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.status.observe(this, Observer { status ->
            status ?: return@Observer
            when (status) {
                LoginDataModel.STATUS_NOT_WORKING -> enableViews()
                LoginDataModel.STATUS_FAILED -> {
                    enableViews()
                    toast("Nie można się zalogować")
                }
                LoginDataModel.STATUS_FAILED_WRONG_INPUTS -> {
                    enableViews()
                    toast("Błędny login lub hasło")
                }
                LoginDataModel.STATUS_WORKING -> disableViews()
                LoginDataModel.STATUS_SUCCESS -> {
                    (activity as? WelcomeActivity?)?.apply {
                        UpdateWorker.requestUpdates(this)
                        MobiregPreferences.get(this).seenWelcomeActivity = true
                        AppUpdateWorker.requestChecks()
                        applyFragment(AfterLoginFragment.newInstance())
                    }
                }
            }
        })

        MobiregPreferences.get(context!!).deviceId

        confirmBtn?.setOnClickListener {
            val login = editLogin?.text?.toString() ?: return@setOnClickListener
            val pass = editPass?.text?.toString() ?: return@setOnClickListener

            val emptyErrorMsg = "To pole nie może być puste"

            val regex = Regex("^([a-zA-Z0-9_\\-]+)\\.([a-zA-Z0-9_\\-]+)$")

            when {
                login.isEmpty() -> editLogin?.error = emptyErrorMsg
                !regex.matches(login) -> editLogin?.error = "Nieprawidłowy login\nLogin musi kończyć się adresem hosta (np jakub.zsl-krakow)"
                pass.isEmpty() -> editPass?.error = emptyErrorMsg
                else -> {
                    val values = regex.matchEntire(login)!!.groupValues
                    if (isNetworkAvailable) {
                        if (activity?.intent?.getBooleanExtra("isPreview", false) != true)
                            viewModel.login(values[1], values[2], MobiregPreferences.encryptPassword(pass))
                        else
                            viewModel.performLogin()
                    } else
                        runNoInternetAnimation()
                }
            }
        }

        confirmBtn?.setOnLongClickListener {
            startActivity(Intent(it.context ?: return@setOnLongClickListener true
                    , SettingsActivity::class.java))
            true
        }

        btnAboutPrivacy?.setOnClickListener {
            makeAboutPrivacyDialog(it.context ?: return@setOnClickListener)
        }
    }

    private fun makeAboutPrivacyDialog(context: Context) {
        AlertDialog.Builder(context)
                .setTitle("O przetwarzaniu danych")
                .setMessage("""Zacznijmy od tego, że aplikacja jest otwartoźródłowa, więc samemu możesz sprawdzić jak działa.
                    |Twoje hasło jest haszowane (bezpowrotnie) metodą MD5 w momencie dotknięcia przycisku Zaloguj się.
                    |W takiej formie jest wysyłane bezpośrednio do serwera mobireg.pl połączeniem https i nigdzie indziej.
                    |Jeżeli logowanie zakończy się sukcesem zarówno login jak i hasło (zahaszowane) są zapisywane w pliku dostępnym tylko dla tej aplikacji.
                    |Dane te są używane w przyszłości do synchronizacji danych z serwerem.
                    |Baza danych uzyskana od mobirega jest przechowywana lokalnie i dostępna tylko dla tej aplikacji.
                    |Nie są zbierane żadne dane statystyczne.
                """.trimMargin())
                .setPositiveButton("Rozumiem") { dialog, _ -> dialog.dismiss() }
                .show()
    }


    private fun runNoInternetAnimation() {
        val noInternetText = this.noInternetText ?: return
        val confirmBtn = this.confirmBtn ?: return
        noInternetText.visibility = View.VISIBLE

        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 500
        animation.interpolator = DecelerateInterpolator()
        animation.repeatCount = 1
        animation.repeatMode = Animation.REVERSE
        animation.fillAfter = true
        noInternetText.startAnimation(animation)

        confirmBtn.isEnabled = false
        confirmBtn.postDelayed({ this.confirmBtn?.isEnabled = true }, 1000)
    }

    private val isNetworkAvailable
        get()
        = (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.activeNetworkInfo?.isConnected == true


    private fun disableViews() {
        editLogin?.isEnabled = false
        editPass?.isEnabled = false

        confirmBtn?.isEnabled = false
        progressBar?.visibility = View.VISIBLE
    }

    private fun enableViews() {
        editLogin?.isEnabled = true
        editPass?.isEnabled = true

        confirmBtn?.isEnabled = true
        progressBar?.visibility = View.INVISIBLE
    }
}
