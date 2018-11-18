package jakubweg.mobishit.fragment

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.FragmentActivity
import jakubweg.mobishit.helper.ThemeHelper
import java.lang.ref.WeakReference

abstract class AttendanceBaseSheetFragment
    : BottomSheetDialogFragment() {

    override fun getTheme() = ThemeHelper.getBottomSheetTheme(context!!)

    fun showSelfInsteadOfMe(me: AttendanceBaseSheetFragment?) = apply {
        show(me?.activity?.supportFragmentManager, null)
        showingSheetOnDetach = WeakReference(me)
        me?.savedInstanceState = true
        me?.dismissAllowingStateLoss()
    }

    fun showSelf(activity: Activity?) = apply {
        show((activity as? FragmentActivity)?.supportFragmentManager, null)
        savedInstanceState = false
    }

    fun dismissWithoutOpeningPrevious() {
        showingSheetOnDetach.get()?.dismissWithoutOpeningPrevious()
        showingSheetOnDetach = WeakReference(null)
        dismissAllowingStateLoss()
    }

    private var showingSheetOnDetach = WeakReference<AttendanceBaseSheetFragment?>(null)
    private var savedInstanceState = false

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        this.savedInstanceState = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedInstanceState = true
    }

    override fun onDetach() {
        super.onDetach()
        if (!savedInstanceState)
            showingSheetOnDetach.get()
                    ?.takeUnless { it.isAdded }
                    ?.showSelf(activity)
    }
}