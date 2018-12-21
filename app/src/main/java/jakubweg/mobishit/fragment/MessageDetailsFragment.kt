package jakubweg.mobishit.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.MessageDao
import jakubweg.mobishit.helper.setLeftDrawable
import jakubweg.mobishit.helper.textView
import jakubweg.mobishit.model.MessageDetailModel
import java.lang.ref.WeakReference

class MessageDetailsFragment : Fragment() {
    companion object {
        fun newInstance(messageId: Int) = newInstance(messageId, null, null)

        fun newInstance(messageId: Int, messageTitle: CharSequence?, transitionName: String?) = MessageDetailsFragment().also {
            it.arguments = Bundle().apply {
                putInt("messageId", messageId)
                putCharSequence("title", messageTitle)
                putString("transitionName", transitionName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
            sharedElementReturnTransition = TransitionInflater.from(context!!).inflateTransition(android.R.transition.move)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_message_details, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val messageId = arguments!!.getInt("messageId")

        view.textView(R.id.textTitle).also {
            val title = arguments!!.getCharSequence("title", null)
            title?.apply { it?.text = this }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            //view.findViewById<View?>(R.id.textTitleHolder)?.transitionName = arguments!!.getString("transitionName")
                it?.transitionName = arguments!!.getString("transitionName")

        }

        view.textView(R.id.textView1)?.setLeftDrawable(R.drawable.ic_short_text)
        view.textView(R.id.textView2)?.setLeftDrawable(R.drawable.ic_person)
        view.textView(R.id.textView3)?.setLeftDrawable(R.drawable.ic_access_time)

        val model = ViewModelProviders.of(this)[MessageDetailModel::class.java]

        model.init(messageId)

        model.details.observe(this, SafeObserver(this))
    }

    private class SafeObserver(v: MessageDetailsFragment)
        : Observer<MessageDao.MessageLongInfo> {
        private val fragment = WeakReference<MessageDetailsFragment>(v)
        override fun onChanged(msg: MessageDao.MessageLongInfo?) {
            msg ?: return
            fragment.get()?.view?.apply {
                textView(R.id.textSender)!!.text = msg.sender?.takeUnless { it.isBlank() } ?: "Bez nadawcy"

                textView(R.id.textDate)!!.text = msg.formattedSendTime

                textView(R.id.textTitle)!!.also {
                    if (it.text.isNullOrEmpty())
                        it.text = MessagesListFragment.makeMessageTitle(msg)
                }

                textView(R.id.textContent)!!.also {
                    it.movementMethod = LinkMovementMethod.getInstance()
                    it.text = msg.content
                }

                findViewById<FloatingActionButton?>(R.id.btnReply)?.setOnClickListener {
                    (fragment.get()?.activity as? MainActivity)
                            ?.applyNewDetailsFragment(ComposeMessageFragment.newInstance(
                                    msg.senderId,
                                    "Re: ${if (msg.kind == MessageDao.KIND_JUST_MESSAGE)
                                        msg.title else "Adnotacja"}"))
                }
            }
        }
    }
}