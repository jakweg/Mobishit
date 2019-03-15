package jakubweg.mobishit.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.fragment.LoginFragment
import jakubweg.mobishit.fragment.WelcomeFirstFragment
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.service.FcmServerNotifierWorker

class WelcomeActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val prefs = MobiregPreferences.get(this)
        if (prefs.isSignedIn && intent?.getBooleanExtra("isPreview", false) == false) {
            Toast.makeText(this, "Użytkownik jest już zalogowany", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (savedInstanceState == null)
            applyFragment(if (!prefs.seenWelcomeActivity)
                WelcomeFirstFragment.newInstance() else LoginFragment.newInstance())
    }

    fun applyFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_in)
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MobiregPreferences.get(this).run { isSignedIn && allowedInstantNotifications }
                && intent?.getBooleanExtra("isPreview", false) == false)
            FcmServerNotifierWorker.requestPeriodicServerNotifications()
    }
}
