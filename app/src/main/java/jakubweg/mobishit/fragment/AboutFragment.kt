package jakubweg.mobishit.fragment

import android.content.Intent
import android.net.Uri
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
import android.widget.ImageView
import jakubweg.mobishit.R


class AboutFragment : Fragment() {
    companion object {
        fun newInstance() = AboutFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    private var clicks = 0
    private var isAnimating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btnOpenGithub)!!.setOnClickListener {
            openGithub()
        }
        view.findViewById<View>(R.id.btnReportError)!!.setOnClickListener {
            sendMessage()
        }
        view.findViewById<View>(R.id.btnUsedLibs)!!.setOnClickListener {
            showUsedLibs()
        }
        view.findViewById<ImageView>(R.id.imgAppIcon)!!.apply {
            setOnClickListener { view->
                if (isAnimating) return@setOnClickListener
                if (clicks++ >= 10) {
                    clicks = 0
                    view.startAnimation(RotateAnimation(0f, 360f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).also {
                        it.duration = 1000
                        it.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationRepeat(animation: Animation?) { }
                            override fun onAnimationEnd(animation: Animation?) {
                                isAnimating = false
                            }
                            override fun onAnimationStart(animation: Animation?) {
                                isAnimating = true
                            }
                        })
                    })
                } else {
                    view.startAnimation(ScaleAnimation(1.05f, 1f, 1.05f, 1f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).also {
                        it.duration = 300
                    })
                }
            }
        }
    }

    private fun openGithub() {
        openLink("https://github.com/JakubekWeg/Mobishit")
    }

    private fun openLink(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }

    private fun sendMessage() {
        AlertDialog.Builder(context ?: return)
                .setMessage("Jak chcesz się skontaktować?")
                .setPositiveButton("Messenger") { i, _ -> i.dismiss(); openLink("https://m.me/jakweg") }
                .setNeutralButton("Mail") { i, _ -> i.dismiss(); openSendEmail() }
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
                .setMessage("""Support librarys from Google:
                    |• support
                    |• appcompat
                    |• constraint-layout
                    |• design
                    |• recyclerview
                    |• preference
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
