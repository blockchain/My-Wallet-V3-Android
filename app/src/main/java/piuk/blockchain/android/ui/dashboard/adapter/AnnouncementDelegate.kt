package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_announcement.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.balance.AnnouncementData
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.extensions.inflate

class AnnouncementDelegate<in T> : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int) = items[position] is AnnouncementData

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            AnnouncementViewHolder(parent.inflate(R.layout.item_announcement))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {
        val announcement = items[position] as AnnouncementData
        val context = (holder as AnnouncementViewHolder).itemView.context

        if (AndroidUtils.is21orHigher()) {
            holder.title.text = "${context.getString(announcement.title)} ${announcement.emoji}"
        } else {
            holder.title.setText(announcement.title)
        }
        holder.description.setText(announcement.description)
        holder.image.setImageDrawable(
                ContextCompat.getDrawable(holder.image.context, announcement.image)
        )
        holder.link.setText(announcement.link)

        holder.close.setOnClickListener { announcement.closeFunction() }
        holder.link.setOnClickListener { announcement.linkFunction() }
    }

    private class AnnouncementViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView = itemView.textview_title
        internal val description: TextView = itemView.textview_content
        internal val image: ImageView = itemView.imageview_icon
        internal val close: ImageView = itemView.imageview_close
        internal val link: TextView = itemView.textview_link
    }

}