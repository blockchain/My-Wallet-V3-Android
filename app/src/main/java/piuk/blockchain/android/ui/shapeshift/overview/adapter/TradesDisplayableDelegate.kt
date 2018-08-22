package piuk.blockchain.android.ui.shapeshift.overview.adapter

import android.app.Activity
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.shapeshift.data.Trade
import kotlinx.android.synthetic.main.item_shapeshift_trade.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.androidcore.data.currency.CurrencyFormatUtil
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.math.BigDecimal
import java.util.Locale

class TradesDisplayableDelegate<in T>(
    activity: Activity,
    private var btcExchangeRate: Double,
    private var ethExchangeRate: Double,
    private var bchExchangeRate: Double,
    private var showCrypto: Boolean,
    private val listClickListener: TradesListClickListener
) : AdapterDelegate<T> {

    private val prefsUtil = PrefsUtil(activity)
    private val currencyFormatUtil = CurrencyFormatUtil()
    private val dateUtil = DateUtil(activity)

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is Trade

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        TradeViewHolder(parent.inflate(R.layout.item_shapeshift_trade))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {

        val viewHolder = holder as TradeViewHolder
        val trade = items[position] as Trade

        if (trade.timestamp > 0) {
            viewHolder.timeSince.text = dateUtil.formatted(trade.timestamp / 1000)
        } else {
            // Existing Web issue - no available date to set
            viewHolder.timeSince.text = ""
        }

        viewHolder.result.text = getDisplaySpannable(
            trade.acquiredCoinType,
            trade.quote?.withdrawalAmount ?: BigDecimal.ZERO
        )

        viewHolder.status.setText(determineStatus(viewHolder, trade))

        viewHolder.result.setOnClickListener {
            showCrypto = !showCrypto
            listClickListener.onValueClicked(showCrypto)
        }

        viewHolder.layout.setOnClickListener { listClickListener.onTradeClicked(trade.quote?.deposit ?: "") }
    }

    fun onViewFormatUpdated(isBtc: Boolean) {
        showCrypto = isBtc
    }

    fun onPriceUpdated(btcExchangeRate: Double, ethExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
        this.ethExchangeRate = ethExchangeRate
    }

    private fun getResolvedColor(viewHolder: RecyclerView.ViewHolder, @ColorRes color: Int): Int =
        ContextCompat.getColor(viewHolder.context, color)

    private fun determineStatus(viewHolder: TradeViewHolder, trade: Trade): Int {
        val pair = trade.quote?.pair
        if (pair.equals("eth_eth", true) ||
            pair.equals("btc_btc", true) ||
            pair.equals("bch_bch", true)
        ) {

            viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
            viewHolder.status.setTextColor(
                getResolvedColor(
                    viewHolder,
                    R.color.product_red_medium
                )
            )
            return R.string.shapeshift_refunded_title
        }

        return when (trade.status) {
            Trade.STATUS.COMPLETE -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_complete)
                viewHolder.status.setTextColor(
                    getResolvedColor(
                        viewHolder,
                        R.color.product_green_medium
                    )
                )
                R.string.shapeshift_complete_title
            }
            Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
                viewHolder.status.setTextColor(
                    getResolvedColor(
                        viewHolder,
                        R.color.product_red_medium
                    )
                )
                R.string.shapeshift_failed_title
            }
            Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_inprogress)
                viewHolder.status.setTextColor(
                    getResolvedColor(
                        viewHolder,
                        R.color.product_gray_transferred
                    )
                )
                R.string.shapeshift_in_progress_title
            }
            else -> throw IllegalStateException("Unknown status ${trade.status}")
        }
    }

    private fun getDisplaySpannable(
        cryptoCurrency: String,
        cryptoAmount: BigDecimal
    ): String {

        val displayAmount: String

        if (showCrypto) {
            val crypto = when (cryptoCurrency.toUpperCase()) {
                CryptoCurrency.ETHER.symbol -> currencyFormatUtil.formatEthWithUnit(cryptoAmount)
                CryptoCurrency.BTC.symbol -> currencyFormatUtil.formatBtcWithUnit(cryptoAmount)
                CryptoCurrency.BCH.symbol -> currencyFormatUtil.formatBchWithUnit(cryptoAmount)
                else -> currencyFormatUtil.formatBtcWithUnit(cryptoAmount) // Coin type not specified
            }

            displayAmount = crypto
        } else {

            val fiatAmount = when (cryptoCurrency.toUpperCase()) {
                CryptoCurrency.ETHER.symbol -> cryptoAmount.multiply(
                    BigDecimal.valueOf(ethExchangeRate)
                )
                CryptoCurrency.BTC.symbol -> cryptoAmount.multiply(
                    BigDecimal.valueOf(btcExchangeRate)
                )
                CryptoCurrency.BCH.symbol -> cryptoAmount.multiply(
                    BigDecimal.valueOf(bchExchangeRate)
                )
                else -> BigDecimal.ZERO // Coin type not specified
            }

            displayAmount = currencyFormatUtil.formatFiatWithSymbol(
                FiatValue(
                    getPreferredFiatUnit(),
                    fiatAmount
                ), Locale.getDefault()
            )
        }

        return displayAmount
    }

    private fun getPreferredFiatUnit() =
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private class TradeViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal val result: TextView = itemView.result
        internal val timeSince: TextView = itemView.date
        internal val status: TextView = itemView.status
        internal val layout: RelativeLayout = itemView.trade_row
    }
}