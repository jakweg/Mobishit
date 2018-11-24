package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.ShareCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import jakubweg.mobishit.BuildConfig
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.FragmentActivity
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.helper.textView


class AboutFragment : Fragment() {
    companion object {
        fun newInstance() = AboutFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    private var clicks = 0
    private var isAnimating = false

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.textView(R.id.textVersionInfo)!!.text = "Mobishit ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - ${BuildConfig.BUILD_TYPE}"


        view.findViewById<View>(R.id.btnOpenGithub)!!.setOnClickListener {
            openGithub()
        }
        view.findViewById<Button>(R.id.btnReportError)!!.apply {
            val ending = when (MobiregPreferences.get(context ?: return).sex) {
                "M" -> "eś"
                "K" -> "aś"
                else -> "aś/eś"
            }

            text = "Masz jakąś sugestię lub znalazł$ending błąd? - skontaktuj się ze mną!"

            setOnClickListener {
                sendMessage()
            }
        }
        view.findViewById<View>(R.id.btnUsedLibs)!!.setOnClickListener {
            showUsedLibs()
        }
        view.findViewById<ImageView>(R.id.imgAppIcon)!!.apply {
            setOnClickListener { view ->
                if (isAnimating) return@setOnClickListener
                if (clicks++ >= 7) {
                    clicks = 0
                    startBiggerAnimation(view)
                } else {
                    view.startAnimation(ScaleAnimation(1.05f, 1f, 1.05f, 1f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).also {
                        it.duration = 300
                    })
                }
            }
        }
    }

    private fun startBiggerAnimation(view: View) {
        view.startAnimation(RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).also {
            it.duration = 1000
            it.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    isAnimating = false
                }

                override fun onAnimationStart(animation: Animation?) {
                    isAnimating = true
                }
            })
        })
    }

    private fun openGithub() {
        (activity as? FragmentActivity?)?.openLink("https://github.com/JakubekWeg/Mobishit")
    }

    private fun sendMessage() {
        AlertDialog.Builder(context ?: return)
                .setTitle("Jak chcesz się ze mną skontaktować?")
                .setPositiveButton("Messenger") { _, _ -> (activity as? FragmentActivity?)?.openLink("https://m.me/jakweg") }
                .setNeutralButton("Telegram") { _, _ -> (activity as? FragmentActivity?)?.openLink("https://t.me/jakweg") }
                .setNegativeButton("Mail") { _, _ -> openSendEmail() }
                .show()
    }

    private fun openSendEmail() {
        val activity = this.activity ?: return
        val intent = ShareCompat.IntentBuilder
                .from(activity)
                .addEmailTo("jakubek.weg@gmail.com")
                .setChooserTitle("Napisz maila")
                .setSubject("O aplikacji Mobishit")
                .setType("message/rfc822")
                .createChooserIntent()!!

        activity.startActivity(intent)
    }

    private fun showUsedLibs() {
        AlertDialog.Builder(activity ?: return)
                .setTitle("Użyte biblioteki")
                .setMessage("""Support libraries from Google:
                    |• support
                    |• appcompat
                    |• constraint-layout
                    |• design
                    |• recyclerview
                    |• preference
                    |
                    |Firebase components from Google:
                    |• core
                    |• messaging
                    |
                    |Architecture components from Google:
                    |• room
                    |• lifecycle
                    |• work
                    |
                    |From others:
                    |• com.google.code.gson
                    |• org.jsoup
                    |• com.squareup.leakcanary
                """.trimMargin())
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
    }


}
