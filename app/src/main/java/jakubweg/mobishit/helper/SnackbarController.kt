package jakubweg.mobishit.helper

import android.animation.ObjectAnimator
import android.os.Build
import android.support.annotation.MainThread
import android.support.constraint.ConstraintLayout
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import jakubweg.mobishit.R
import java.lang.ref.WeakReference

class SnackbarController(
        private val layout: ConstraintLayout) {
    private val messageText = layout.textView(R.id.snackbar_text)!!
    private val action = layout.findViewById<Button>(R.id.snackbar_action)!!

    private val animationLength = 300L
    private var isAnimating = false
    private var wasAnimating = false

    init {
        layout.viewTreeObserver.addOnGlobalLayoutListener(LayoutObserver(this))
    }

    private class LayoutObserver(c: SnackbarController?) : ViewTreeObserver.OnGlobalLayoutListener {
        private val mController = WeakReference<SnackbarController>(c)
        override fun onGlobalLayout() {
            val controller = mController.get() ?: return
            if (Build.VERSION.SDK_INT < 16) {
                @Suppress("DEPRECATION")
                controller.layout.viewTreeObserver.removeGlobalOnLayoutListener(this)
            } else {
                controller.layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
            controller.onLayoutSizeChanged()
        }
    }

    private fun makeEnterAnimation()
            : ObjectAnimator = ObjectAnimator.ofFloat(layout, "translationY", 0F).apply { duration = animationLength }

    private fun makeExitAnimation()
            : ObjectAnimator = ObjectAnimator.ofFloat(layout, "translationY", layout.height.toFloat()).apply { duration = animationLength }

    private fun makeInstantExitAnimation() = makeExitAnimation().apply { duration = 0L }

    private fun onLayoutSizeChanged() {
        if (!wasAnimating) {
            makeInstantExitAnimation().start()
            wasAnimating = true
            layout.postDelayed({ onLayoutSizeChanged() }, 20L)
        }

        if (isAnimating)
            return

        val item = pendingRequests.firstOrNull() ?: return
        if (!item.mIsCancelled) {
            item.controller = WeakReference(this)
            isAnimating = true
            makeEnterAnimation().start()
            layout.postDelayed({
                // entered!

                if (item.length >= 0) {
                    layout.postDelayed({
                        // snackbar timeout!
                        hideCurrentElement(item)

                    }, item.length)
                }
            }, animationLength)
        }
    }

    private fun hideCurrentElement(item: ShowRequest) {
        if (!item.mIsCancelled) {
            makeExitAnimation().start()
            layout.postDelayed({
                // gone forever
                item.mIsCancelled = true
                item.controller = WeakReference<SnackbarController>(null)
                isAnimating = false
                pendingRequests.remove(item)
                executeRequest()

            }, animationLength)
        }
    }

    interface OnActionClickedListener {
        fun onClicked()
    }

    class WeakClickedListener(runnable: Runnable)
        : OnActionClickedListener {
        private val mRunnable = WeakReference<Runnable>(runnable)
        override fun onClicked() {
            mRunnable.get()?.run()
        }
    }

    class StrongClickedListener(private val runnable: Runnable)
        : OnActionClickedListener {
        override fun onClicked() {
            runnable.run()
        }
    }

    class ShowRequest(val message: CharSequence,
                      val buttonText: CharSequence?,
                      val length: Long,
                      val onActionClickedListener: OnActionClickedListener?) {

        constructor(message: CharSequence, length: Long) : this(
                message, null, length, null)

        internal var mIsCancelled = false
        internal var controller = WeakReference<SnackbarController>(null)

        @MainThread
        fun cancel() {
            if (mIsCancelled) return
            controller.get()?.hideCurrentElement(this)
            mIsCancelled = true
        }
    }

    private val pendingRequests = mutableListOf<ShowRequest>()

    @MainThread
    fun show(request: ShowRequest) {
        if (request.mIsCancelled)
            return

        pendingRequests.add(request)
        if (pendingRequests.size == 1)
            executeRequest()
    }

    @MainThread
    fun showCancelingCurrent(request: ShowRequest) {
        cancelCurrent()
        show(request)
    }

    private fun executeRequest() {
        var item: ShowRequest? = null
        while (pendingRequests.isNotEmpty()) {
            item = pendingRequests.first()
            if (item.mIsCancelled)
                pendingRequests.removeAt(0)
            else
                break
        }

        item ?: return

        messageText.text = item.message
        if (item.buttonText == null) {
            action.visibility = View.GONE
            action.text = ""
        } else {
            action.visibility = View.VISIBLE
            action.text = item.buttonText
            action.setOnClickListener { item.onActionClickedListener?.onClicked(); item.cancel() }
        }
        // waiting for compute size
        onLayoutSizeChanged()
    }

    @MainThread
    fun cancelCurrentIfIndefinite() {
        pendingRequests.firstOrNull()?.apply {
            if (length < 0)
                cancel()
        }
    }

    @MainThread
    fun cancelCurrent() {
        pendingRequests.firstOrNull()?.cancel()
    }

    @MainThread
    fun cancelAll() {
        pendingRequests.forEach { it.cancel() }
    }


    val isShowingAnything get() = pendingRequests.isNotEmpty()
}