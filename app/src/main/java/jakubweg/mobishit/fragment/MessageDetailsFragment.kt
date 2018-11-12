package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Build
import android.os.Bundle
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.MessageDao
import jakubweg.mobishit.model.MessageDetailModel
import java.lang.ref.WeakReference

class MessageDetailsFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(messageId: Int) = newInstance(messageId, null, null)

        @JvmStatic
        fun newInstance(messageId: Int, messageTitle: CharSequence?, transitionName: String?) = MessageDetailsFragment().also {
            it.arguments = Bundle().apply {
                putInt("messageId", messageId)
                putCharSequence("title", messageTitle)
                putString("transitionName", transitionName)
            }
        }
    }

    fun showSelf(activity: MainActivity?) {
        (activity as? DoublePanelActivity)?.applyNewDetailsFragment(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
            sharedElementReturnTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
        = inflater.inflate(R.layout.fragment_message_details, container, false)



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val messageId = arguments!!.getInt("messageId")

        view.findViewById<TextView>(R.id.textTitle).also {
            val title = arguments!!.getCharSequence("title", null)
            title?.apply { it.text = this }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                it.transitionName = arguments!!.getString("transitionName")

        }

        val model = ViewModelProviders.of(this)[MessageDetailModel::class.java]

        model.init(messageId)

        model.details.observe(this, SafeObserver(view))
    }

    private class SafeObserver(v: View)
        : Observer<MessageDao.MessageLongInfo> {
        private val view = WeakReference<View>(v)
        override fun onChanged(msg: MessageDao.MessageLongInfo?) {
            msg ?: return
            view.get()?.apply {
                findViewById<TextView>(R.id.textSender)!!.text = msg.sender?.takeUnless { it.isBlank() } ?: "Bez nadawcy"

                findViewById<TextView>(R.id.textSendDate)!!.text = msg.formattedSendTime

                findViewById<TextView>(R.id.textTitle)!!.also {
                    if (it.text.isNullOrEmpty())
                        it.text = MessagesListFragment.makeMessageTitle(msg)
                }

                findViewById<TextView>(R.id.textContent)!!.text = msg.content

            }
        }
    }
}