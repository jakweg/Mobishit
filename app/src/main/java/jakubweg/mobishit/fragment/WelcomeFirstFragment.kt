package jakubweg.mobishit

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.widget.TextView
import jakubweg.mobishit.activity.WelcomeActivity
import jakubweg.mobishit.helper.MobiregAdjectiveManager
import java.lang.ref.WeakReference

class WelcomeFirstFragment : Fragment() {
    companion object {
        fun newInstance() = WelcomeFirstFragment()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_first_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adjective = view.findViewById<TextView>(R.id.text3)!!

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

        view.findViewById<View>(R.id.btnFinish).setOnClickListener {
            (it.context as? WelcomeActivity?)?.applyFragment(LoginFragment.newInstance())
        }
    }
}
