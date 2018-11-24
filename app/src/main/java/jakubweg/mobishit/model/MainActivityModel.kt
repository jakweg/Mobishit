package jakubweg.mobishit.model

import android.arch.lifecycle.ViewModel
import jakubweg.mobishit.fragment.MarksViewOptionsFragment
import java.lang.ref.WeakReference

class MainActivityModel : ViewModel() {

    var mOptionListeners = mutableSetOf<WeakReference<MarksViewOptionsFragment.OptionsChangedListener>>()
    fun addOptionListener(listener: MarksViewOptionsFragment.OptionsChangedListener?) {
        mOptionListeners.add(WeakReference(listener ?: return))
    }

//    fun removeOptionListener(listener: MarksViewOptionsFragment.OptionsChangedListener?) {
//        listener ?: return
//        val iterator = mOptionListeners.iterator()
//        while (iterator.hasNext()) {
//            val l = iterator.next().get()
//            if (l == null)
//                iterator.remove()
//            else if (l == listener) {
//                iterator.remove()
//                return
//            }
//        }
//    }
}