package piuk.blockchain.android.ui.buysell.overview.models

import android.support.annotation.StringRes
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import java.util.Date

/**
 * Marker interface for all RecyclerView classes
 */
interface BuySellDisplayable

/**
 * To display Buy/Sell Buttons in the RecyclerView
 */
class BuySellButtons : BuySellDisplayable

/**
 * Represents zero transactions in the RecyclerView
 */
class EmptyTransactionList : BuySellDisplayable

/**
 * Represents a KYC review still in progress
 */
class KycInProgress : BuySellDisplayable

/**
 * Represents a currently active recurring buy order/subscription.
 */
data class RecurringBuyOrder(
    val displayString: String,
    val subscriptionId: Int
) : BuySellDisplayable

/**
 * Wrapper for buy/sell transactions
 */
data class BuySellTransaction(
    val transactionId: Int,
    val time: Date,
    val displayAmount: String,
    @StringRes val tradeStateString: Int,
    val isSellTransaction: Boolean,
    val tradeState: TradeState
) : BuySellDisplayable