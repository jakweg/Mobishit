package jakubweg.mobishit.fragment

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.button.MaterialButton
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import jakubweg.mobishit.R
import jakubweg.mobishit.db.MessageDao
import jakubweg.mobishit.helper.MobiregPreferences
import jakubweg.mobishit.model.ComposeMessageModel
import jakubweg.mobishit.service.MessageUploadWorker


@Suppress("NOTHING_TO_INLINE")
class ComposeMessageFragment : Fragment() {
    companion object {
        fun newInstance() = newInstance(0)

        fun newInstance(receiverId: Int) = ComposeMessageFragment().apply {
            arguments = Bundle().also {
                it.putInt("receiver", receiverId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_compose_mail, container, false).also {
            if (!MobiregPreferences.get(context!!).allowedInstantNotifications)
                it.postDelayed({
                    AlertDialog.Builder(context ?: return@postDelayed)
                            .setMessage("Do wysyłania wiadomości używany jest dedykowany serwer.\n" +
                                    "Musisz wyrazić zgodę na korzystanie z niego, aby kontynuować.\n" +
                                    "Twoje dane nadal będą bezpieczne.")
                            .setPositiveButton("Wyrażam zgodę") { _, _ ->
                                val context = context ?: return@setPositiveButton
                                MobiregPreferences.get(context).apply {
                                    allowedInstantNotifications = true
                                }
                                Toast.makeText(context, "Wyrażono zgodę", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Anuluj") { _, _ ->
                                isDiscarding = true
                                fragmentManager?.popBackStack()
                            }
                            .show()

                }, 300L)
        }
    }

    private inline fun View.btn(id: Int) = findViewById<Button>(id)!!

    private inline fun View.edit(id: Int) = findViewById<AppCompatEditText>(id)!!

    private inline val viewModel get() = ViewModelProviders.of(this)[ComposeMessageModel::class.java]

    private var isDiscarding = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!isDiscarding) {
            outState.putCharSequence("subject", view?.edit(R.id.editSubject)?.text)
            outState.putCharSequence("content", view?.edit(R.id.editContent)?.text)
            outState.putInt("selectedTeacherId", viewModel.selectedTeacherId)
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onDestroyView() {
        super.onDestroyView()
        context?.getSharedPreferences("mail", Context.MODE_PRIVATE)?.edit()?.also {
            if (!isDiscarding) {
                it.putInt("selectedTeacherId", viewModel.selectedTeacherId)
                it.putString("subject", view?.edit(R.id.editSubject)?.text?.toString())
                it.putString("content", view?.edit(R.id.editContent)?.text?.toString())
            } else {
                it.clear()
            }
        }?.commit() // we use commit and don't change it

        val view = activity?.currentFocus
        if (view != null) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return

        if (savedInstanceState != null) {
            viewModel.selectedTeacherId = savedInstanceState.getInt("selectedTeacherId", 0)
            savedInstanceState.getCharSequence("subject", null)?.also {
                view.edit(R.id.editSubject).text = SpannableStringBuilder(it)
            }
            savedInstanceState.getCharSequence("content", null)?.also {
                view.edit(R.id.editContent).text = SpannableStringBuilder(it)
            }
        } else {
            context.getSharedPreferences("mail", Context.MODE_PRIVATE)?.also { prefs ->
                viewModel.selectedTeacherId = prefs.getInt("selectedTeacherId", 0)
                prefs.getString("subject", null)?.also {
                    view.edit(R.id.editSubject).text = SpannableStringBuilder(it)
                }
                prefs.getString("content", null)?.also {
                    view.edit(R.id.editContent).text = SpannableStringBuilder(it)
                }
            }
        }


        viewModel.teachers.observe(this, Observer { teachers ->
            teachers ?: return@Observer
            if (viewModel.selectedTeacherId != 0) {
                viewModel.selectedTeacher = teachers.find { it.id == viewModel.selectedTeacherId }
                viewModel.selectedTeacher?.also { view.btn(R.id.btnChooseReceiver).text = it.fullName }
            }
        })



        view.btn(R.id.btnChooseReceiver).setOnClickListener { btn ->
            viewModel.teachers.value?.also { teachers ->
                val names = Array(teachers.size) { teachers[it].fullName }
                AlertDialog.Builder(context)
                        .setTitle("Wybierz odbiorcę wiadomości")
                        .setItems(names) { _, pos ->
                            (btn as? MaterialButton?)?.text = names[pos]
                            viewModel.selectedTeacher = teachers[pos]
                            viewModel.selectedTeacherId = viewModel.selectedTeacher!!.id
                        }
                        .setNegativeButton("Anuluj", null)
                        .show()
            }
        }

        view.btn(R.id.btnChooseReceiver).setOnLongClickListener { btn ->
            val textView = EditText(btn.context)
            textView.hint = "ID użytkownika"
            viewModel.selectedTeacher?.id?.toString()?.also { textView.text = SpannableStringBuilder(it) }
            textView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            AlertDialog.Builder(context)
                    .setTitle("Wybierz odbiorcę wiadomości po id")
                    .setView(textView)
                    .setPositiveButton("Zapisz") { _, _ ->
                        textView.text.toString().toIntOrNull()?.also { id ->
                            viewModel.selectedTeacherId = id
                            viewModel.selectedTeacher = MessageDao.TeacherIdAndName(id, "Nieznany $id").also {
                                (btn as? MaterialButton?)?.text = it.fullName
                            }
                        }
                    }
                    .setNegativeButton("Anuluj", null)
                    .show()
            true
        }

        view.btn(R.id.btnSend).setOnClickListener {
            if (viewModel.selectedTeacher == null) {
                Toast.makeText(context, "Nie wybrano odbiorcy", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val subject = view.edit(R.id.editSubject).text
            if (subject.isNullOrBlank()) {
                view.edit(R.id.editSubject).error = "Za krótki tytuł\nNapisz coś więcej"
                return@setOnClickListener
            }
            if (subject.length > 120) {
                view.edit(R.id.editSubject).error = "Za długi tytuł"
                return@setOnClickListener
            }

            val content = view.edit(R.id.editContent).text
            if (content.isNullOrBlank()) {
                view.edit(R.id.editContent).error = "Za krótka treść wiadomości"
                return@setOnClickListener
            }
            if (content.length > 2000) {
                view.edit(R.id.editContent).error = "Za długa treść wiadomości"
                return@setOnClickListener
            }

            val ending = when (MobiregPreferences.get(context).sex) {
                "M" -> "y"
                "K" -> "a"
                else -> "a/y"
            }
            AlertDialog.Builder(context)
                    .setMessage("Wiadomość zostanie wysłana do ${viewModel.selectedTeacher?.fullName
                            ?: " nieznajomego"}")
                    .setTitle("Jesteś tego pewn$ending?")
                    .setNegativeButton("Anuluj", null)
                    .setPositiveButton("Wyślij") { _, _ ->
                        MessageUploadWorker.requestMessageSent(context,
                                viewModel.selectedTeacherId,
                                subject.toString(), content.toString())
                        fragmentManager?.popBackStack()
                        isDiscarding = true
                        Toast.makeText(context, "Wysyłanie wiadomości...", Toast.LENGTH_SHORT).show()
                    }
                    .show()
        }

        view.btn(R.id.btnDiscard).setOnClickListener {
            AlertDialog.Builder(context)
                    .setMessage("Wiadomość zostanie odrzucona")
                    .setPositiveButton("OK") { _, _ ->
                        isDiscarding = true
                        fragmentManager?.popBackStack()
                    }
                    .setNegativeButton("Anuluj", null)
                    .show()
        }
    }
}