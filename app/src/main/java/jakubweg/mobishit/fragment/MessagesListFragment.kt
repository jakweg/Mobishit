package jakubweg.mobishit

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.db.MessageDao
import jakubweg.mobishit.helper.DateHelper
import jakubweg.mobishit.helper.EmptyAdapter
import jakubweg.mobishit.model.MessagesListModel
import java.lang.ref.WeakReference

class MessagesListFragment : Fragment() {
    companion object {
        fun newInstance() = MessagesListFragment()

        fun makeMessageTitle(it: MessageDao.MessageLongInfo) = makeMessageTitle(it.title, it.kind, it.sender)

        private fun makeMessageTitle(it: MessageDao.MessageShortInfo) = makeMessageTitle(it.title, it.kind, it.sender)

        private fun makeMessageTitle(title: String?, kind: Int, @Suppress("UNUSED_PARAMETER") sender: String?): String {
            return title ?: (when (kind) {
                MessageDao.KIND_NEUTRAL_REPRIMAND -> "Informacja"
                MessageDao.KIND_POSITIVE_REPRIMAND -> "Uwaga pozytywna"
                MessageDao.KIND_NEGATIVE_REPRIMAND -> "Uwaga negatywna"
                MessageDao.KIND_JUST_MESSAGE -> "Wiadomość"
                else -> "Dziwna wiadomość"
            }/* + " od $sender"*/)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_messages, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model = ViewModelProviders.of(this)[MessagesListModel::class.java]

        val list = view.findViewById<RecyclerView>(R.id.messagesList)

        model.messages.observe(this, SafeObserver(list))
    }

    private class SafeObserver(v: RecyclerView)
        : Observer<List<MessageDao.MessageShortInfo>> {
        private val messagesList = WeakReference<RecyclerView>(v)

        override fun onChanged(messages: List<MessageDao.MessageShortInfo>?) {
            messages ?: return
            messagesList.get()?.apply {
                adapter = if (messages.isEmpty())
                    EmptyAdapter("Brak wiadomości")
                else
                    MessageAdapter(context!!, messages) { id, title, view ->
                        (context!! as? DoublePanelActivity)?.applyNewDetailsFragment(
                                view, MessageDetailsFragment.newInstance(id, title, ViewCompat.getTransitionName(view)))
                    }
            }
        }
    }

    private class MessageAdapter(context: Context, private val list: List<MessageDao.MessageShortInfo>,
                                 private val onMessageClicked: ((Int, CharSequence, View) -> Unit)? = null)
        : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        private val inflater = LayoutInflater.from(context)!!
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(inflater.inflate(R.layout.message_list_item, parent, false))

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also {
                holder.messageTitle.text = makeMessageTitle(it)
                holder.messageInfo.text = "${it.sender} \u2022 ${DateHelper.millisToStringTime(it.sendTime)}"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    holder.messageTitle.transitionName = "me$position"

            }
        }


        private fun onItemClicked(pos: Int, title: CharSequence, view: View) {
            val item = list[pos]
            onMessageClicked?.invoke(item.id, title, view)
        }


        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val messageTitle = v.findViewById<TextView>(R.id.messageTitle)!!
            val messageInfo = v.findViewById<TextView>(R.id.messageInfo)!!

            init {
                v.setOnClickListener { onItemClicked(adapterPosition, messageTitle.text, messageTitle) }
            }
        }
    }
}