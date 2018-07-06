package piuk.blockchain.android.ui.account.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_accounts_row_header.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.AccountItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class CreateWalletDelegate<in T>(
    val listener: AccountHeadersListener
) : AdapterDelegate<T> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        HeaderViewHolder(parent.inflate(R.layout.item_accounts_row_header))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        val headerViewHolder = holder as HeaderViewHolder
        headerViewHolder.bind(items[position] as AccountItem, listener)
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        if (items[position] is AccountItem) {
            (items[position] as AccountItem).type == AccountItem.TYPE_CREATE_NEW_WALLET_BUTTON ||
                (items[position] as AccountItem).type == AccountItem.TYPE_WALLET_HEADER
        } else {
            false
        }

    private class HeaderViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val header: TextView = itemView.header_name
        private val plus: ImageView = itemView.imageview_plus

        internal fun bind(
            item: AccountItem,
            listener: AccountHeadersListener
        ) {
            header.setText(R.string.wallets)
            plus.visible()

            if (item.type != AccountItem.TYPE_WALLET_HEADER) {
                itemView.setOnClickListener { listener.onCreateNewClicked() }
            } else {
                itemView.setOnClickListener(null)
                plus.gone()
            }
        }
    }
}