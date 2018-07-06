package piuk.blockchain.androidbuysell.models.coinify

/**
 * Currencies are ISO_4217 Strings, eg "USD", "BTC". Coinify's API is a little strange, so the rules
 * are as follows:
 *
 * If you're sending money, eg sending 1.0 BTC in exchange for GBP, the [baseCurrency] is BTC, the
 * [quoteCurrency] is GBP and the [baseAmount] value must be negative. The [Quote.quoteAmount]
 * received in return is positive, because that's the amount you're receiving.
 *
 * If you are sending BTC in exchange for GBP and want £100 worth, [baseCurrency] is GBP,
 * [quoteCurrency] is BTC and the [baseAmount] value is positive. The corresponding
 * [Quote.baseAmount] returned will be negative, because that's the amount you'll need to send.
 *
 * @see [https://en.wikipedia.org/wiki/ISO_4217].
 */
internal data class QuoteRequest(
    val baseCurrency: String,
    val quoteCurrency: String,
    val baseAmount: Double
)

/**
 * A quote returned from the /trades/quote endpoint. [id] is optional; it won't be returned if the
 * request wasn't authenticated. Currencies are ISO_4217 Strings, eg "USD", "BTC". Times are
 * ISO_8601, eg "2016-04-01T12:27:36Z".
 *
 * @see [https://en.wikipedia.org/wiki/ISO_4217]
 * @see [https://en.wikipedia.org/wiki/ISO_8601]
 */
data class Quote(
    val id: Int?,
    val baseCurrency: String,
    val quoteCurrency: String,
    val baseAmount: Double,
    val quoteAmount: Double,
    val issueTime: String,
    val expiryTime: String
)