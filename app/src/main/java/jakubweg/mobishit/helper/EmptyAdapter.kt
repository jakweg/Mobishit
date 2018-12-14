package jakubweg.mobishit.helper

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jakubweg.mobishit.R

class EmptyAdapter constructor(private val text: CharSequence,
                               private val matchParent: Boolean) : RecyclerView.Adapter<EmptyAdapter.ViewHolder>() {

    constructor(text: CharSequence) : this(text, true)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    if (matchParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
            this.text = this@EmptyAdapter.text
            val padding = parent.context!!.resources.getDimensionPixelSize(R.dimen.snackbarLayoutPadding)
            setPadding(padding, padding, padding, padding)
            textSize = 18f //sp
        })
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}
