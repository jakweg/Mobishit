package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = MobiregPreferences.get(context ?: return)

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
            (it.context as Activity?)?.apply {
                finish()
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}
