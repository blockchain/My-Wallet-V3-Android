package com.blockchain.ui.chooser

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.utils.extensions.goneIf

class AccountChooserAdapter(
    private val items: List<AccountChooserItem>,
    private val clickEvent: (Any) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun clickListener(account: Any?): View.OnClickListener = View.OnClickListener {
        if (account != null) {
            clickEvent(account)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (ViewType.values().first { it.ordinal == viewType }) {
            ViewType.VIEW_TYPE_HEADER -> {
                val header =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_accounts_row_header, parent, false)
                HeaderViewHolder(header)
            }
            ViewType.VIEW_TYPE_CONTACT -> {
                val contact = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
                ContactViewHolder(contact)
            }
            ViewType.VIEW_TYPE_ACCOUNT -> {
                val account = LayoutInflater.from(parent.context).inflate(R.layout.item_accounts_row, parent, false)
                AccountViewHolder(account)
            }
            ViewType.VIEW_TYPE_LEGACY -> {
                val account = LayoutInflater.from(parent.context).inflate(R.layout.item_accounts_row, parent, false)
                AddressViewHolder(account)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is AccountChooserItem.Header -> {
                val headerViewHolder = holder as HeaderViewHolder
                headerViewHolder.header.text = item.label
                holder.itemView.setOnClickListener(null)
            }
            is AccountChooserItem.Contact -> {
                val contactViewHolder = holder as ContactViewHolder
                contactViewHolder.name.text = item.label
                holder.itemView.setOnClickListener(clickListener(item.accountObject))
            }
            is AccountChooserItem.AccountSummary -> {
                (holder as AccountViewHolder).apply {
                    label.text = item.label
                    balance.text = item.displayBalance
                }
                holder.itemView.setOnClickListener(clickListener(item.accountObject))
            }
            is AccountChooserItem.LegacyAddress -> {
                (holder as AddressViewHolder).apply {
                    label.text = item.label
                    balance.text = item.displayBalance
                    address.text = item.address
                    tag.setText(R.string.watch_only)
                    address.goneIf(item.address == null)
                    tag.goneIf(item.address == null || !item.isWatchOnly)
                }
                holder.itemView.setOnClickListener(clickListener(item.accountObject))
            }
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        when (items[position]) {
            is AccountChooserItem.Header -> ViewType.VIEW_TYPE_HEADER
            is AccountChooserItem.Contact -> ViewType.VIEW_TYPE_CONTACT
            is AccountChooserItem.AccountSummary -> ViewType.VIEW_TYPE_ACCOUNT
            is AccountChooserItem.LegacyAddress -> ViewType.VIEW_TYPE_LEGACY
        }.ordinal
}

private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var header: TextView = itemView.findViewById(R.id.header_name)

    init {
        itemView.findViewById<View>(R.id.imageview_plus).visibility = View.GONE
    }
}

private class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var name: TextView = itemView.findViewById(R.id.contactName)

    init {
        itemView.findViewById<View>(R.id.contactStatus).visibility = View.GONE
        itemView.findViewById<View>(R.id.imageviewIndicator).visibility = View.GONE
        itemView.findViewById<View>(R.id.imageViewMore).visibility = View.GONE
    }
}

private class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal var label: TextView = itemView.findViewById(R.id.my_account_row_label)
    internal var balance: TextView = itemView.findViewById(R.id.my_account_row_amount)

    init {
        itemView.findViewById<View>(R.id.my_account_row_tag).visibility = View.GONE
        itemView.findViewById<View>(R.id.my_account_row_address).visibility = View.GONE
    }
}

private class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal var label: TextView = itemView.findViewById(R.id.my_account_row_label)
    internal var tag: TextView = itemView.findViewById(R.id.my_account_row_tag)
    internal var balance: TextView = itemView.findViewById(R.id.my_account_row_amount)
    internal var address: TextView = itemView.findViewById(R.id.my_account_row_address)
}

private enum class ViewType {
    VIEW_TYPE_HEADER,
    VIEW_TYPE_CONTACT,
    VIEW_TYPE_ACCOUNT,
    VIEW_TYPE_LEGACY
}
