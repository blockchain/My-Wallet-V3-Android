package piuk.blockchain.android.ui.account.adapter

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.item_accounts_row.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.AccountItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class AccountDelegate<in T>(
    val listener: AccountHeadersListener
) : AdapterDelegate<T> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AccountViewHolder(parent.inflate(R.layout.item_accounts_row))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        val accountViewHolder = holder as AccountViewHolder
        accountViewHolder.bind(items[position] as AccountItem, listener)
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        if (items[position] is AccountItem) {
            (items[position] as AccountItem).type == AccountItem.TYPE_ACCOUNT_BTC ||
                (items[position] as AccountItem).type == AccountItem.TYPE_ACCOUNT_BCH ||
                (items[position] as AccountItem).type == AccountItem.TYPE_LEGACY_SUMMARY
        } else {
            false
        }

    private class AccountViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.my_account_row_label
        private val address: TextView = itemView.my_account_row_address
        private val amount: TextView = itemView.my_account_row_amount
        private val tag: TextView = itemView.my_account_row_tag

        internal fun bind(accountItem: AccountItem, listener: AccountHeadersListener) {

            if (accountItem.type == AccountItem.TYPE_LEGACY_SUMMARY) {
                itemView.setOnClickListener(null)
                title.text = context.getString(R.string.imported_addresses)
                address.gone()
                tag.gone()
                amount.apply {
                    text = accountItem.amount
                    setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.product_green_medium
                        )
                    )
                }
            } else {
                // Normal account view
                itemView.setOnClickListener {
                    listener.onAccountClicked(
                        if (accountItem.type == AccountItem.TYPE_ACCOUNT_BCH) {
                            CryptoCurrency.BCH
                        } else {
                            CryptoCurrency.BTC
                        },
                        accountItem.correctedPosition
                    )
                }

                title.text = accountItem.label

                if (!accountItem.address.isNullOrEmpty()) {
                    address.apply {
                        visible()
                        text = accountItem.address
                    }
                } else {
                    address.gone()
                }

                if (accountItem.isArchived) {
                    amount.apply {
                        setText(R.string.archived_label)
                        setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.product_gray_transferred
                            )
                        )
                    }
                } else {
                    amount.apply {
                        text = accountItem.amount
                        setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.product_green_medium
                            )
                        )
                    }
                }

                if (accountItem.isWatchOnly) {
                    tag.apply {
                        setText(R.string.watch_only)
                        setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.product_red_medium
                            )
                        )
                    }
                }

                if (accountItem.isDefault) {
                    tag.apply {
                        setText(R.string.default_label)
                        setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.product_gray_transferred
                            )
                        )
                    }
                }

                if (!accountItem.isWatchOnly && !accountItem.isDefault) {
                    tag.gone()
                } else {
                    tag.visible()
                }
            }
        }
    }
}