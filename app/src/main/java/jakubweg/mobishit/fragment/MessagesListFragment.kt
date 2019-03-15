package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import jakubweg.mobishit.R
import jakubweg.mobishit.activity.DoublePanelActivity
import jakubweg.mobishit.activity.MainActivity
import jakubweg.mobishit.db.AppDatabase
import jakubweg.mobishit.db.MessageDao
import jakubweg.mobishit.helper.*
import jakubweg.mobishit.model.MessagesListModel

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_messages, container, false)

    private inline val viewModel
        get() = ViewModelProviders.of(this)[MessagesListModel::class.java]

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabReselected(it: TabLayout.Tab?) = Unit
        override fun onTabSelected(it: TabLayout.Tab?) {
            it ?: return
            view?.findViewById<RecyclerView?>(R.id.messagesList)?.adapter = null
            val infoText = view?.findViewById<TextView?>(R.id.sentMessagesInfo)
            when (it.position) {
                0 -> {
                    infoText?.visibility = View.GONE
                    viewModel.sentMessagesLiveData.removeObserver(sentMessagesListObserver)
                    viewModel.sentMessages.removeObserver(sentMessagesObserver)
                    viewModel.receivedMessages.observe(this@MessagesListFragment, receivedMessagesObserver)
                }
                1 -> {
                    infoText?.visibility = View.VISIBLE
                    viewModel.receivedMessages.removeObserver(receivedMessagesObserver)
                    viewModel.sentMessagesLiveData.observe(this@MessagesListFragment, sentMessagesListObserver)
                }
            }
        }

        override fun onTabUnselected(it: TabLayout.Tab?) = Unit
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedItemPos", selectedItemPos)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<RecyclerView?>(R.id.messagesList)?.apply {
            val dividerItemDecoration = DividerItemDecoration(context,
                    (layoutManager as LinearLayoutManager).orientation)
            addItemDecoration(dividerItemDecoration)
        }

        view.findViewById<TextView?>(R.id.sentMessagesInfo)?.setLeftDrawable(R.drawable.nav_info)

        view.findViewById<TabLayout>(R.id.tabLayout)?.also { tabLayout ->
            val selectedItemPos = savedInstanceState?.getInt("selectedItemPos", 0)
                    ?.takeIf { it >= 0 } ?: 0
            tabLayout.addOnTabSelectedListener(tabSelectedListener)
            tabLayout.getTabAt(selectedItemPos)?.also { tab ->
                tab.select()
                tabSelectedListener.onTabSelected(tab)
            }
        }

        view.findViewById<FloatingActionButton?>(R.id.btnNewMessage)?.setOnClickListener {
            (activity as? MainActivity?)?.applyNewDetailsFragment(ComposeMessageFragment.newInstance())
        }
    }

    private val sentMessagesListObserver = Observer<Long> {
        viewModel.sentMessages.observe(this, sentMessagesObserver)
    }

    private fun RecyclerView.startListAnimation() {
        startAnimation(AlphaAnimation(0f, 1f).also { it.duration = 300 })
    }

    private val selectedItemPos
        get() = view?.findViewById<TabLayout>(R.id.tabLayout)?.selectedTabPosition ?: -1

    private val receivedMessagesObserver
        get() = Observer<List<MessageDao.MessageShortInfo>> { messages ->
            if (selectedItemPos != 0) return@Observer
            view?.apply {
                findViewById<RecyclerView?>(R.id.messagesList)?.apply {
                    startListAnimation()
                    adapter = if (messages.isNullOrEmpty())
                        EmptyAdapter("Brak wiadomości", true)
                    else
                        ReceivedMessagesAdapter(context!!, messages)
                }
            }
        }

    private val sentMessagesObserver
        get() = Observer<List<MessageDao.SentMessageShortData>> { messages ->
            if (selectedItemPos != 1) return@Observer
            view?.apply {
                val listView = findViewById<RecyclerView?>(R.id.messagesList) ?: return@apply
                listView.startListAnimation()
                if (messages == null) {
                    listView.adapter = EmptyAdapter("Ładowanie wiadomości")
                } else if (messages.isEmpty()) {
                    listView.adapter = EmptyAdapter("Brak wysłanych wiadomości")
                } else {
                    val adapter = listView.adapter as? SentMessagesAdapter?
                    if (adapter == null)
                        listView.adapter = SentMessagesAdapter(context!!).also { it.setShownData(messages) }
                    else
                        adapter.setShownData(messages)
                }
            }
        }

    private class SentMessagesAdapter(context: Context)
        : RecyclerView.Adapter<SentMessagesAdapter.ViewHolder>() {
        private val inflater = LayoutInflater.from(context)!!
        private var messages = listOf<MessageDao.SentMessageShortData>()

        fun setShownData(messages: List<MessageDao.SentMessageShortData>) {
            val oldMessages = this.messages
            this.messages = messages
            if (oldMessages.size == messages.size - 1)
                notifyItemInserted(0)
            else
                notifyDataSetChanged()
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.sent_message_list_item, p0, false))
        }

        override fun getItemCount() = messages.size

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            val item = messages[pos]
            holder.title.precomputedText = item.subject
            holder.subtitle.precomputedText = "${item.statusAsString()} • ${DateHelper.millisToStringTime(item.sentTime)} " +
                    "\u2022 ${item.fullName ?: "nieznany odbiorca ${item.receiverId}"}"
        }


        private inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.findViewById<TextView>(R.id.title)!!
            val subtitle = v.findViewById<TextView>(R.id.subtitle)!!

            init {
                v.setOnClickListener {
                    AlertDialog.Builder(inflater.context)
                            .setTitle("Wysłana wiadomość")
                            .setMessage(AppDatabase.getAppDatabase(inflater.context)
                                    .messageDao.getSentMessageContent(messages[adapterPosition].id)) // yeah, database reading on main thread
                            .setPositiveButton("Zamknij", null)
                            .show()
                }
            }
        }
    }

    private class ReceivedMessagesAdapter(private val context: Context,
                                          private val list: List<MessageDao.MessageShortInfo>)
        : RecyclerView.Adapter<ReceivedMessagesAdapter.ViewHolder>() {
        private val inflater = LayoutInflater.from(context)!!
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(inflater.inflate(R.layout.message_list_item, parent, false))

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            list[position].also { msg ->
                holder.messageTitle.precomputedText = makeMessageTitle(msg).takeUnless { it.isBlank() } ?: "(Bez tytułu)"
                holder.messageInfo.precomputedText = "${msg.sender} \u2022 ${DateHelper.millisToStringTime(msg.sendTime)}"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    holder.messageTitle.transitionName = "me$position"
                holder.colorView.setBackgroundColor(when (msg.kind) {
                    MessageDao.KIND_JUST_MESSAGE -> Color.LTGRAY
                    MessageDao.KIND_NEUTRAL_REPRIMAND -> Color.DKGRAY
                    MessageDao.KIND_NEGATIVE_REPRIMAND -> -6750208
                    MessageDao.KIND_POSITIVE_REPRIMAND -> -16751104
                    else -> Color.LTGRAY
                })
            }
        }


        private fun onItemClicked(pos: Int, title: CharSequence, view: View) {
            val item = list[pos]

            (context as? DoublePanelActivity)?.applyNewDetailsFragment(
                    view, MessageDetailsFragment.newInstance(item.id, title, ViewCompat.getTransitionName(view)))
        }


        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val messageTitle = v.textView(R.id.messageTitle)!!
            val messageInfo = v.textView(R.id.messageInfo)!!
            val colorView = v.findViewById<View>(R.id.messageColor)!!

            init {
                v.setOnClickListener { onItemClicked(adapterPosition, messageTitle.text, messageTitle) }
            }
        }
    }
}