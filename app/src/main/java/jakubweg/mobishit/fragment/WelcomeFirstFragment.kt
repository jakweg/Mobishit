package jakubweg.mobishit.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v4.text.HtmlCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.widget.TextView
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.WelcomeActivity
import jakubweg.mobishit.helper.DedicatedServerManager
import jakubweg.mobishit.helper.MobiregAdjectiveManager
import jakubweg.mobishit.helper.precomputedText
import jakubweg.mobishit.helper.textView
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class WelcomeFirstFragment : Fragment() {
    companion object {
        fun newInstance() = WelcomeFirstFragment()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_first_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.textView(R.id.textRules)!!.apply {
            precomputedText = HtmlCompat.fromHtml("Klikając <em>ROZPOCZNIJ</em> akceptujesz\n" +
                    "<u>Politykę Prywatności i Regulamin korzystania</u>.",
                    HtmlCompat.FROM_HTML_MODE_COMPACT)
            setOnClickListener {
                // I know it is bad
                thread {
                    try {
                        val link = DedicatedServerManager(context!!).termsOfUseLink!!
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context
                                    ?: return@post, "Wystąpił błąd", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }


        val adjective = view.textView(R.id.text3)!!

        val animation = AlphaAnimation(0f, 1f)
        val weakView = WeakReference<TextView>(adjective)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) = Unit
            override fun onAnimationStart(animation: Animation?) = Unit
            var isShown = false
            override fun onAnimationRepeat(animation: Animation?) {
                if (isShown) {
                    weakView.get()?.also {
                        it.text = MobiregAdjectiveManager.getRandom()
                    }
                }
                isShown = !isShown
            }

        })
        animation.duration = 1500L
        animation.interpolator = Interpolator {
            return@Interpolator if (it > 0.66f)
                1f
            else
                it * 3f
        }
        animation.repeatCount = Animation.INFINITE
        animation.repeatMode = Animation.REVERSE
        adjective.startAnimation(animation)

        view.findViewById<View>(R.id.btnFinish)?.setOnClickListener {
            (activity as? WelcomeActivity?)?.applyFragment(LoginFragment.newInstance())
        }
    }
}
