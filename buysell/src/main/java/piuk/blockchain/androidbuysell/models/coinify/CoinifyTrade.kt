package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString

data class CoinifyTrade(
    /** Unique ID for this trade */
    val id: Int,
    /** Reference to the trader that created the trade */
    val traderId: Int,
    /** The current state of the trade */
    val state: TradeState,
    /** Currency (ISO 4217) denominating [inAmount] */
    val inCurrency: String,
    /** Currency (ISO 4217) denominating [outAmount] or [outAmountExpected] */
    val outCurrency: String,
    /** The amount of [inAmount] that this trade covers. Is always positive. */
    val inAmount: Double,
    /**
     * (Optional) The amount of outCurrency that this trade resulted in. Is always positive.
     * NOTE: This field is only defined if the state of the trade is completed. For all other
     * states, see the [outAmountExpected] field.
     */
    val outAmount: Double?,
    /** The amount of outCurrency that this trade is expected to result in. Is always positive. */
    val outAmountExpected: Double,
    /** Object describing how Coinify will receive/have received the money to fund the trade. */
    val transferIn: Transfer,
    /** Object describing how Coinify will send/have sent the result of the trade back to the trader. */
    val transferOut: Transfer,
    /** (Optional). URL to a printable receipt for this trade. This field is present if [state] is complete. */
    val receiptUrl: String?,
    /**
     * (Optional) The time when the price quote underlying this trade expires. NOTE: This field
     * is only defined in the following states: awaiting_transfer_in, processing, reviewing.
     */
    val quoteExpireTime: String?,
    /** The time when the trade was last updated (ISO 8601). */
    val updateTime: String,
    /** Timestamp for when this trade was first created (ISO 8601). */
    val createTime: String,
    /** The ID of the subscription which triggered this trade, if applicable */
    val tradeSubscriptionId: Int? = null
) {

    /**
     * Returns whether or not this trade involves selling cryptocurrency for fiat.
     *
     * @return True if the trade was a sale of cryptocurrency.
     */
    fun isSellTransaction(): Boolean =
        inCurrency.equals("btc", true) || inCurrency.equals("eth", true)
}

data class Transfer(
    /** Unique identifier for this transfer. */
    val id: Int,
    /** State of this transfer */
    val state: TransferState,
    /** Currency (ISO 4217) that this transfer is/will be denominated in. */
    val currency: String,
    /** Amount that is/will be sent to this transfer. Denominated in [currency]. Is always positive. */
    val sendAmount: Double,
    /**
     * Amount that this transfer will result in. Denominated in [currency]. Is always positive
     * and equal to or smaller then [sendAmount]. The difference between the [sendAmount] and
     * the [receiveAmount] will be the fee of the transfer.
     */
    val receiveAmount: Double,
    /** Transfer medium. The medium transferring the currency. */
    val medium: Medium,
    /**
     * Reference to either Coinify's internal accounts or the user's account. Note at the
     * moment Coinify only have internal bank accounts.
     */
    val mediumReceiveAccountId: Int,
    /** Information relevant for this medium */
    val details: Details
) {
    /**
     * Returns the fee for this transfer.
     *
     * @return A [Double] which is the fee paid in this [Transfer].
     */
    fun getFee(): Double = sendAmount - receiveAmount
}

sealed class TransferState {
    // Waiting to receive money, or waiting for signal to send money
    object Waiting : TransferState()

    // Transfer completed
    object Completed : TransferState()

    // Test transfer completed
    object CompletedTest : TransferState()

    // Transfer cancelled
    object Cancelled : TransferState()

    // Transfer expired
    object Expired : TransferState()

    // Transfer rejected
    object Rejected : TransferState()

    // Transfer is in review
    object Reviewing : TransferState()

    // You will potentially see this state shortly after making a sell trade, when the crypto funds have been sent
    // but Coinify haven't seen it yet
    object Unknown : TransferState()
}

@Suppress("unused")
class TransferStateAdapter {

    @FromJson
    fun fromJson(input: String): TransferState = when (input) {
        WAITING -> TransferState.Waiting
        COMPLETED -> TransferState.Completed
        COMPLETED_TEST -> TransferState.CompletedTest
        CANCELLED -> TransferState.Cancelled
        EXPIRED -> TransferState.Expired
        REJECTED -> TransferState.Rejected
        REVIEWING -> TransferState.Reviewing
        UNKNOWN -> TransferState.Unknown
        else -> throw JsonDataException("Unknown transfer state object $input, unsupported data type")
    }

    @ToJson
    fun toJson(transferState: TransferState): String = when (transferState) {
        TransferState.Waiting -> WAITING
        TransferState.Completed -> COMPLETED
        TransferState.CompletedTest -> COMPLETED_TEST
        TransferState.Cancelled -> CANCELLED
        TransferState.Expired -> EXPIRED
        TransferState.Rejected -> REJECTED
        TransferState.Reviewing -> REVIEWING
        TransferState.Unknown -> UNKNOWN
    }

    private companion object {
        private const val WAITING = "waiting"
        private const val COMPLETED = "completed"
        private const val COMPLETED_TEST = "completed_test"
        private const val CANCELLED = "cancelled"
        private const val EXPIRED = "expired"
        private const val REJECTED = "rejected"
        private const val REVIEWING = "reviewing"
        private const val UNKNOWN = "unknown"
    }
}

// Marker interface for payment details
interface Details

data class BlockchainDetails(
    /** Either the trader's or Coinify's bitcoin address */
    val account: String,
    /** The BTC transaction that sent out the BTC to the above address. Not present if unconfirmed */
    val eventData: EventData? = null
) : Details

data class EventData(
    /** The transaction hash */
    val txId: String,
    /** Timestamp for when this trade was completed (ISO 8601). */
    val receiveTime: String
)

data class CardDetails(
    /**	String identifying the PSP. Current possible values are (‘isignthis’,'isignthis-staging’,'paylike’) */
    val provider: String? = null,
    /** String identifying the PSP’s merchant. Only relevant when provider='paylike’ */
    val providerMerchantId: String? = null,
    /**
     * Id of the external payment. (For iSignThis, its the transaction id). The paymentId is
     * used for the integration mode: embedded mode.
     */
    val paymentId: String,
    /**	Reference to the card payment. */
    val cardPaymentId: Int? = null,
    /**
     * The return URL to which the user to be sent back after the payment has been created.
     * Can be provided when creating a trade. Only relevant when provider = 'isignthis’ or
     * provider = 'isignthis-staging’
     */
    val returnUrl: String? = null,
    /**
     * Redirect URL to process the payment. Only relevant when provider = 'isignthis’ or
     * provider = 'isignthis-staging’. The redirectUrl is used for the integration mode:
     * redirect mode.
     */
    val redirectUrl: String
) : Details

data class BankDetails(
    /**
     * Text that the bank transfer must contain in order for Coinify to correctly register
     * what trade it concerns.
     */
    val referenceText: String,
    /** Object with additional information about the bank account. */
    val account: Account,
    /** Object with additional information about the bank. */
    val bank: Bank,
    /** Object with additional information about the bank account holder. */
    val holder: Holder,
    /** The time when the bank account was last updated (ISO 8601). */
    val updateTime: String? = null,
    /** The time when the bank account was created (ISO 8601). */
    val createTime: String? = null
) : Details

// For parsing only
data class DetailsJson(
    val account: Any?,
    val tx: String?,
    val provider: String?,
    val providerMerchantId: String?,
    val paymentId: String?,
    val cardPaymentId: Int?,
    val returnUrl: String?,
    val redirectUrl: String?,
    val referenceText: String?,
    val bank: Bank?,
    val holder: Holder?,
    val updateTime: String?,
    val createTime: String?,
    val eventData: EventData?
) : Details

data class Account(
    /** Currency of the bank account */
    val currency: String,
    /** Type of the bank account, can be "danish", "sepa" or "international */
    val type: String? = null,
    /** For sepa and international its the SWIFT / BIC number and for danish accounts its the REG number. */
    val bic: String,
    /**
     * For sepa and international it’s the IBAN (International Bank Account Number). For
     * danish accounts, it’s the BBAN (Basic Bank Account Number).
     */
    val number: String
)

data class Bank(
    val name: String? = null,
    val address: Address
)

data class Holder(
    val name: String,
    val address: Address
)

@Suppress("unused")
class DetailsAdapter {

    private var moshi: Moshi = Moshi.Builder().build()!!

    @FromJson
    fun fromJson(detailsJson: DetailsJson): Details {
        if (detailsJson.account != null && detailsJson.bank == null) {
            // Blockchain Details
            return BlockchainDetails(
                detailsJson.account.toString(),
                detailsJson.eventData
            )
        } else if (detailsJson.bank != null) {
            // Bank Details
            return BankDetails(
                detailsJson.referenceText ?: "Coinify Sandbox Ref",
                moshi.adapter(Account::class.java)
                    .run { fromJson(detailsJson.account!!.toSerialisedString()) }!!,
                detailsJson.bank,
                detailsJson.holder!!,
                detailsJson.updateTime,
                detailsJson.createTime
            )
        } else if (detailsJson.paymentId != null) {
            // Card Details
            return CardDetails(
                detailsJson.provider,
                detailsJson.providerMerchantId,
                detailsJson.paymentId,
                detailsJson.cardPaymentId,
                detailsJson.returnUrl,
                detailsJson.redirectUrl!!
            )
        } else throw JsonDataException("Unknown details object $detailsJson, unsupported data type")
    }

    @ToJson
    fun toJson(details: Details): String = when (details) {
        is BankDetails -> moshi.adapter(BankDetails::class.java).toJson(details)
        is CardDetails -> moshi.adapter(CardDetails::class.java).toJson(details)
        is BlockchainDetails -> moshi.adapter(BlockchainDetails::class.java).toJson(details)
        else -> throw JsonDataException("Unknown details object $details, unsupported data type")
    }
}

sealed class Medium {

    /**
     * Transfer on a public blockchain, such as Bitcoin. NOTE: The currency field of the transfer
     * object will determine which blockchain currency. For now we only support trading with bitcoin.
     */
    object Blockchain : Medium()

    /** Card payment */
    object Card : Medium()

    /**
     * Bank account payment. If transfer is incoming we sent our own bank details as the details
     * object. And if transfer is outgoing its the details of the traders bank account.
     */
    object Bank : Medium()
}

@Suppress("unused")
class MediumAdapter {

    @FromJson
    fun fromJson(input: String): Medium = when (input) {
        BANK -> Medium.Bank
        CARD -> Medium.Card
        BLOCKCHAIN -> Medium.Blockchain
        else -> throw JsonDataException("Unknown medium $input, unsupported data type")
    }

    @ToJson
    fun toJson(medium: Medium): String = when (medium) {
        Medium.Bank -> BANK
        Medium.Card -> CARD
        Medium.Blockchain -> BLOCKCHAIN
    }

    private companion object {
        private const val BANK = "bank"
        private const val CARD = "card"
        private const val BLOCKCHAIN = "blockchain"
    }
}

sealed class TradeState {
    // (Starting state) Trade was successfully created, and is waiting for trader’s payment
    object AwaitingTransferIn : TradeState()

    // Trade has received the trader’s payment, and we are processing the trade.
    object Processing : TradeState()

    // Trade is undergoing manual review
    object Reviewing : TradeState()

    // (Ending state) Trade completed successfully.
    object Completed : TradeState()

    // (Ending state) Trade completed successfully on TestNet
    object CompletedTest : TradeState()

    // (Ending state) Trade cancelled.
    object Cancelled : TradeState()

    // (Ending state) Trade was rejected.
    object Rejected : TradeState()

    // (Ending state) Trade expired before it completed.
    object Expired : TradeState()

    // (Ending state) Trade is being refunded
    object Refunded : TradeState()

    fun isEndState(): Boolean = (this === Completed ||
        this === CompletedTest ||
        this === Cancelled ||
        this === Rejected ||
        this === Refunded ||
        this === Expired)

    fun isFailureState(): Boolean = (this === Cancelled ||
        this === Rejected ||
        this === Refunded ||
        this === Expired)

    override fun toString(): String = when (this) {
        TradeState.AwaitingTransferIn -> TradeStateAdapter.AWAITING_TRANSFER_IN
        TradeState.Processing -> TradeStateAdapter.PROCESSING
        TradeState.Reviewing -> TradeStateAdapter.REVIEWING
        TradeState.Completed -> TradeStateAdapter.COMPLETED
        TradeState.CompletedTest -> TradeStateAdapter.COMPLETED_TEST
        TradeState.Cancelled -> TradeStateAdapter.CANCELLED
        TradeState.Rejected -> TradeStateAdapter.REJECTED
        TradeState.Expired -> TradeStateAdapter.EXPIRED
        TradeState.Refunded -> TradeStateAdapter.REFUNDED
    }
}

@Suppress("unused")
class TradeStateAdapter {

    @FromJson
    fun fromJson(input: String): TradeState = when (input) {
        AWAITING_TRANSFER_IN -> TradeState.AwaitingTransferIn
        PROCESSING -> TradeState.Processing
        REVIEWING -> TradeState.Reviewing
        COMPLETED -> TradeState.Completed
        COMPLETED_TEST -> TradeState.CompletedTest
        CANCELLED -> TradeState.Cancelled
        REJECTED -> TradeState.Rejected
        EXPIRED -> TradeState.Expired
        REFUNDED -> TradeState.Refunded
        else -> throw JsonDataException("Unknown trade state $input, unsupported data type")
    }

    @ToJson
    fun toJson(tradeState: TradeState): String = when (tradeState) {
        TradeState.AwaitingTransferIn -> AWAITING_TRANSFER_IN
        TradeState.Processing -> PROCESSING
        TradeState.Reviewing -> REVIEWING
        TradeState.Completed -> COMPLETED
        TradeState.CompletedTest -> COMPLETED_TEST
        TradeState.Cancelled -> CANCELLED
        TradeState.Rejected -> REJECTED
        TradeState.Expired -> EXPIRED
        TradeState.Refunded -> REFUNDED
    }

    internal companion object {
        const val AWAITING_TRANSFER_IN = "awaiting_transfer_in"
        const val PROCESSING = "processing"
        const val REVIEWING = "reviewing"
        const val COMPLETED = "completed"
        const val COMPLETED_TEST = "completed_test"
        const val CANCELLED = "cancelled"
        const val REJECTED = "rejected"
        const val EXPIRED = "expired"
        const val REFUNDED = "refunded"
    }
}
