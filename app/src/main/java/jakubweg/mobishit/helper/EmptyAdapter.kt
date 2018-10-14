package jakubweg.mobishit.helper

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class EmptyAdapter constructor(private val text: CharSequence) : RecyclerView.Adapter<EmptyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER
            this.text = this@EmptyAdapter.text
            textSize = 18f //sp
        })
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit


    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

}
