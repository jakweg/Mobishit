package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.CheckBox
import com.google.android.gms.common.ConnectionResult.SUCCESS
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GoogleApiAvailabilityLight
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.helper.MobiregPreferences

class AfterLoginFragment : Fragment() {
    companion object {
        fun newInstance() = AfterLoginFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_after_login, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MobiregPreferences.get(activity ?: return).apply {
            allowedInstantNotifications = view?.findViewById<CheckBox>(R.id.useFcmCheckbox)?.isChecked ?: return@apply
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = MobiregPreferences.get(context ?: return)

        val googlePlayStatus = GoogleApiAvailabilityLight.getInstance()
                .isGooglePlayServicesAvailable(context ?: return)

        if (googlePlayStatus != SUCCESS) {
            AlertDialog.Builder(activity ?: return)
                    .setMessage("Nie można połączyć z usługami Google Play, powiadomienia mogą przychodzić bardzo rzadko lub wcale, ale nadal można korzystać z aplikacji")
                    .setCancelable(false)
                    .setPositiveButton("Pokaż domyślny komunikat") { d, _ ->
                        d.dismiss()
                        GoogleApiAvailability.getInstance().showErrorDialogFragment(activity, googlePlayStatus, 0)
                    }
                    .setNegativeButton("Ok, trudno") { d, _ -> d.dismiss() }
                    .show()
        }

        val welcomeText = view.findViewById<AppCompatTextView>(R.id.welcomeText)!!

        welcomeText.text = "Witaj ${prefs.name}"
        welcomeText.startAnimation(AnimationSet(true).also { set ->
            set.addAnimation(AlphaAnimation(0f, 1f).also {
                it.duration = 1000
            })
            set.addAnimation(ScaleAnimation(0.5f, 1f, 0.5f, 1f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f).also {
                it.duration = 1000
            })
        })

        view.findViewById<View>(R.id.contentLayout)!!.apply {
            postDelayed({
                visibility = View.VISIBLE
                startAnimation(AlphaAnimation(0f, 1f).also {
                    it.duration = 1000
                })
            }, 1000)
        }

        view.findViewById<View>(R.id.btnFinish)!!.setOnClickListener {
            (activity as? Activity?)?.apply {
                finish()
                prefs.decidedAboutFcm = true
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        view.findViewById<CheckBox>(R.id.useFcmCheckbox)?.setOnCheckedChangeListener { checkBox, isChecked ->
            if (!isChecked) {
                val ending = when (prefs.sex) {
                    "M" -> "y"
                    "K" -> "a"
                    else -> "a/y"
                }

                val context = context ?: return@setOnCheckedChangeListener

                AlertDialog.Builder(context)
                        .setIcon(AppCompatResources.getDrawable(context, R.drawable.ic_notifications_off_black))
                        .setCancelable(false)
                        .setTitle("Czy na pewno?")
                        .setMessage("Gdy wyłączysz tę funkcję, zostaniesz powiadomion$ending tylko raz na kilka godzin.\n" +
                                "Czy jesteś pewn$ending takiej decyzji?")
                        .setPositiveButton("Nie, chcę powiadomienia!") { d, _ ->
                            d.dismiss()
                            checkBox?.isChecked = true
                        }
                        .setNegativeButton("Tak, jestem pewn$ending", null)
                        .show()

            }
        }

        view.findViewById<View>(R.id.serverInfoBtn)?.setOnClickListener {
            AlertDialog.Builder(context ?: return@setOnClickListener)
                    .setTitle("Po co serwer?")
                    .setMessage("""API Mobirega nie pozwala na natychmiastowe powiadomienia, ale możemy sprawdzać np. co 5 minut czy coś nie zmieniło.
                        |Sprawdzanie tak często rozładowałoby twoją baterię w bardzo krótkim czasie, dlatego stworzyliśmy specjalny serwer, który sprawdza co chwilę za ciebie.
                        |Gdy coś się zmieni to powiadamia twój telefon, aby on mógł powiadomić Ciebie.
                        |Twoje dane są przesyłane tylko szyfrowanym połączeniem do bezpiecznego źródła gdzie służą tylko synchronizacji.
                    """.trimMargin())
                    .setPositiveButton("Rozumiem") { d, _ -> d.dismiss() }
                    .show()
        }
    }
}
