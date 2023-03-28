@file:Suppress("unused")

package jakubweg.mobishit.activity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.util.Log
import android.view.View
import jakubweg.mobishit.R
import java.util.*

@SuppressLint("Registered")
abstract class DoublePanelActivity : FragmentActivity() {

    abstract val mainFragmentContainerId: Int
    open val secondFragmentContainerId: Int get() = mainFragmentContainerId

    abstract val quitOnBackButton: Boolean
    private var mainFragmentBackStack = ArrayList<String>()

    private var isVisibleActivity = false
    override fun onResume() {
        super.onResume()
        isVisibleActivity = true
    }

    override fun onPause() {
        super.onPause()
        isVisibleActivity = false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null
                && supportFragmentManager.fragments.size == 0) {
            requestNewMainFragment()
        }
    }

    private fun applyNewMainFragment(fragment: Fragment) {
        if (hasSavedInstance) {
            findViewById<View>(mainFragmentContainerId)?.postDelayed({
                applyNewMainFragment(fragment)
            }, 50L)
            return
        }

        supportFragmentManager.clearStack()
        val trans = supportFragmentManager.beginTransaction()
        val newFragmentTag = UUID.randomUUID().toString()
        trans.apply {
            setCustomAnimations(R.anim.fragment_enter, R.anim.fade_out, R.anim.fade_in, R.anim.fragment_exit)
            mainFragmentBackStack.lastOrNull()?.also { prevFragmentTag ->
                supportFragmentManager.findFragmentByTag(prevFragmentTag)?.also { prevFragment ->
                    detach(prevFragment)
                }
            }
            add(mainFragmentContainerId, fragment, newFragmentTag)
        }

        mainFragmentBackStack
            .map { tag -> Pair(tag, supportFragmentManager.findFragmentByTag(tag)) }
            .filter { it.second != null }
            .firstOrNull { it.second!!::class == fragment::class }
            ?.also {
                mainFragmentBackStack.remove(it.first)
                trans.remove(it.second!!)
            }

        trans.commitAllowingStateLoss()
        mainFragmentBackStack.add(newFragmentTag)
    }

    private fun restoreMainFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.fragment_enter, R.anim.fade_out, R.anim.fade_in, R.anim.fragment_exit)
            attach(fragment)
            commitAllowingStateLoss()
        }
        onMainFragmentRestored(fragment)
    }

    abstract fun onMainFragmentRestored(fragment: Fragment)

    fun requestNewMainFragment() {
        createCurrentMainFragment()?.also { applyNewMainFragment(it) }
    }

    fun applyNewDetailsFragment(fragment: Fragment) {
        if (hasSavedInstance) {
            findViewById<View>(secondFragmentContainerId)?.postDelayed({
                applyNewDetailsFragment(fragment)
            }, 50L)
            return
        }

        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fade_out, R.anim.fade_in, R.anim.fragment_exit)
                .replace(secondFragmentContainerId, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    fun applyNewDetailsFragment(sharedView: View, fragment: Fragment) {
        if (hasSavedInstance) {
            findViewById<View>(secondFragmentContainerId)?.postDelayed({
                applyNewDetailsFragment(sharedView, fragment)
            }, 50L)
            return
        }

        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.fragment_enter, R.anim.fade_out, R.anim.fade_in, R.anim.fragment_exit)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                addSharedElement(sharedView, sharedView.transitionName)
            replace(secondFragmentContainerId, fragment)
            addToBackStack(null)
            commitAllowingStateLoss()
        }
    }

    override fun onBackPressed() {
        if(quitOnBackButton)
            super.onBackPressed()
        else {
            if (supportFragmentManager.backStackEntryCount != 0)
                super.onBackPressed()
            else {
                if (mainFragmentBackStack.size >= 2) {
                    val oldMainFragmentTag = mainFragmentBackStack[mainFragmentBackStack.size - 1]

                    if(supportFragmentManager.findFragmentByTag(oldMainFragmentTag) == null)
                        Log.e("BackStack", "Didn't find old fragment. Stack size: ${mainFragmentBackStack.size}, tag: $oldMainFragmentTag")
                    supportFragmentManager.findFragmentByTag(oldMainFragmentTag)?.also{ oldFrag ->
                        supportFragmentManager.beginTransaction().remove(oldFrag).commit()
                    }

                    val newMainFragmentTag = mainFragmentBackStack[mainFragmentBackStack.size - 2]
                    mainFragmentBackStack.removeAt(mainFragmentBackStack.size - 1)
                    if(supportFragmentManager.findFragmentByTag(newMainFragmentTag) == null)
                        Log.e("BackStack", "Didn't find new fragment. Stack size: ${mainFragmentBackStack.size}, tag: $newMainFragmentTag")
                    supportFragmentManager.findFragmentByTag(newMainFragmentTag)?.also{ frag ->
                        restoreMainFragment(frag)
                    }
                }
            }
        }
    }

    private fun FragmentManager.clearStack() = this.apply {
        try {
            while (popBackStackImmediate()) {
            }
        } catch (e: Exception) {
            e.printStackTrace() //c***
        }
    }

    private var hasSavedInstance = false
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putStringArrayList("mainFragmentBackStack", mainFragmentBackStack)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        hasSavedInstance = false
        savedInstanceState?.getStringArrayList("mainFragmentBackStack")?.also{ mainFragmentBackStack = it}
    }

    abstract fun createCurrentMainFragment(): Fragment?
}