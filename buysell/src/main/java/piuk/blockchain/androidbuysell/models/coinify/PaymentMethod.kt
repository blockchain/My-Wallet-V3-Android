package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

data class PaymentMethod(
    // The medium for the in transfer of the trade - by what way the customer pays
    val inMedium: Medium,
    // The medium for the out transfer of the trade - what way the customer receives funds
    val outMedium: Medium,
    // A human-readable name/description of the payment method
    val name: String,
    // 	The possible currencies in which the incoming amount can be made, optionally filtered by request arguments
    val inCurrencies: List<String>,
    // (Optional, if inCurrency parameter provided) Echo of inCurrency parameter.
    val inCurrency: String?,
    // The possible currencies in which the outgoing amount can be made, optionally filtered by request arguments
    val outCurrencies: List<String>,
    // (Optional, if outCurrency parameter provided) Echo of outCurrency parameter.
    val outCurrency: String?,
    // Object of [inCurrencies] and the minimum limit for each.
    val minimumInAmounts: MinimumInAmounts,
    // Object of [inCurrencies] and the maximum limit for each.
    val limitInAmounts: LimitInAmounts,
    // Object of outCurrencies and the trader’s current limit for each currency, based on the current limits of the trader.
    val limitOutAmounts: LimitInAmounts?,
    // Object of [inCurrencies] and fixed fees for each currency for the in transfer.
    val inFixedFees: InFixedFees,
    // Percentage fee for the in transfer.
    val inPercentageFee: Double,
    // Object of [outCurrencies] and fixed fees for each currency for the out transfer.
    val outFixedFees: OutFixedFees,
    // Percentage fee for the out transfer.
    val outPercentageFee: Double,
    /**
     * Can this trader create new trades for this payment method? Note: Only included for authenticated requests.
     * If inCurrency, inAmount, outCurrency and outAmount are all provided in request, this value determines if
     * a trade with the specific amounts/currencies can be made. Otherwise, this value determines if any trade
     * can be made with this payment method.
     *
     * Generally speaking all requests should be authenticated, so you can treat property as non-null.
     */
    val canTrade: Boolean,
    /**
     * (Optional) List of reason objects why the trader cannot create new trades (why canTrade is false).
     * Note: Only included for authenticated requests if canTrade is false.
     *
     * Generally speaking all requests should be authenticated, so you can treat property as non-null.
     */
    val cannotTradeReasons: List<CannotTradeReason>?
)

data class InFixedFees(
    @field:Json(name = "DKK") val dkk: Double,
    @field:Json(name = "EUR") val eur: Double,
    @field:Json(name = "USD") val usd: Double,
    @field:Json(name = "GBP") val gbp: Double,
    @field:Json(name = "BTC") val btc: Double
)

data class MinimumInAmounts(
    @field:Json(name = "DKK") val dkk: Double,
    @field:Json(name = "EUR") val eur: Double,
    @field:Json(name = "USD") val usd: Double,
    @field:Json(name = "GBP") val gbp: Double,
    @field:Json(name = "BTC") val btc: Double
) {
    fun getLimitsForCurrency(currencyCode: String): Double = when (currencyCode.toLowerCase()) {
        "dkk" -> dkk
        "eur" -> eur
        "usd" -> usd
        "gbp" -> gbp
        "btc" -> btc
        else -> throw IllegalArgumentException("Unknown currency code $currencyCode")
    }
}

data class LimitInAmounts(
    @field:Json(name = "DKK") val dkk: Double?,
    @field:Json(name = "EUR") val eur: Double?,
    @field:Json(name = "USD") val usd: Double?,
    @field:Json(name = "GBP") val gbp: Double?,
    @field:Json(name = "BTC") val btc: Double?
) {
    fun getLimitsForCurrency(currencyCode: String): Double = when (currencyCode.toLowerCase()) {
        "dkk" -> dkk
        "eur" -> eur
        "usd" -> usd
        "gbp" -> gbp
        "btc" -> btc
        else -> throw IllegalArgumentException("Unknown currency code $currencyCode")
    }!!
}

data class OutFixedFees(
    @field:Json(name = "BTC") val btc: Double
)

sealed class CannotTradeReason

/**
 * Trader must wait until a specific time before creating trade.
 *
 * @param delayEnd ISO-8601 timestamp for when the delay is over.
 */
data class ForcedDelay(
    val reasonCode: String = CannotTradeReasonAdapter.FORCED_DELAY,
    val delayEnd: String
) : CannotTradeReason()

/**
 * Trader must wait until a specific trade has completed.
 *
 * @param tradeId ID of trade that must be completed
 */
data class TradeInProgress(
    val reasonCode: String = CannotTradeReasonAdapter.TRADE_IN_PROGRESS,
    val tradeId: Int
) : CannotTradeReason()

/**
 * Creating trade would exceed the trader’s limits.
 */
data class LimitsExceeded(
    val reasonCode: String = CannotTradeReasonAdapter.LIMITS_EXCEEDED
) : CannotTradeReason()

/**
 * Contains every possible value for custom type adapter [CannotTradeReasonAdapter].
 */
data class CannotTradeReasonJson(
    val reasonCode: String,
    val delayEnd: String? = null,
    val tradeId: Int? = null
)

@Suppress("unused")
class CannotTradeReasonAdapter {

    @FromJson
    fun fromJson(json: CannotTradeReasonJson): CannotTradeReason = when (json.reasonCode) {
        FORCED_DELAY -> ForcedDelay(json.reasonCode, json.delayEnd!!)
        TRADE_IN_PROGRESS -> TradeInProgress(json.reasonCode, json.tradeId!!)
        LIMITS_EXCEEDED -> LimitsExceeded(json.reasonCode)
        else -> throw JsonDataException("Unknown CannotTradeReason ${json.reasonCode}, unsupported data type")
    }

    @ToJson
    fun toJson(cannotTradeReason: CannotTradeReason): CannotTradeReasonJson =
        when (cannotTradeReason) {
            is ForcedDelay -> CannotTradeReasonJson(
                cannotTradeReason.reasonCode,
                delayEnd = cannotTradeReason.delayEnd
            )
            is TradeInProgress -> CannotTradeReasonJson(
                cannotTradeReason.reasonCode,
                tradeId = cannotTradeReason.tradeId
            )
            is LimitsExceeded -> CannotTradeReasonJson(
                cannotTradeReason.reasonCode
            )
        }

    internal companion object {
        internal const val FORCED_DELAY = "forced_delay"
        internal const val TRADE_IN_PROGRESS = "trade_in_progress"
        internal const val LIMITS_EXCEEDED = "limits_exceeded"
    }
}