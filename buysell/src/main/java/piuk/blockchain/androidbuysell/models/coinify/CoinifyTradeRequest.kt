package piuk.blockchain.androidbuysell.models.coinify

/**
 * An object used to create a final trade with Coinify. Requires a previously fetched [Quote]
 * with a valid quote ID.
 */
class CoinifyTradeRequest private constructor(
    /**
     * Identifier of valid price quote to base the trade on. Passing an invalid price quote
     * will result in an error.
     */
    val priceQuoteId: Int,
    /**
     * An object describing how Coinify will receive the money for this transfer.
     */
    val transferIn: SimpleTransfer,
    /**
     * An object describing how Coinify will send the result of the trade to the trader.
     */
    val transferOut: SimpleTransfer
) {

    companion object {

        fun cardBuy(priceQuoteId: Int, receiveAddress: String): CoinifyTradeRequest =
            CoinifyTradeRequest(
                priceQuoteId,
                SimpleTransfer(Medium.Card),
                SimpleTransfer(Medium.Blockchain, BlockchainDetails(receiveAddress))
            )

        fun bankBuy(priceQuoteId: Int, receiveAddress: String): CoinifyTradeRequest =
            CoinifyTradeRequest(
                priceQuoteId,
                SimpleTransfer(Medium.Bank),
                SimpleTransfer(Medium.Blockchain, BlockchainDetails(receiveAddress))
            )

        fun sell(priceQuoteId: Int, mediumReceiveAccountId: Int): CoinifyTradeRequest =
            CoinifyTradeRequest(
                priceQuoteId,
                SimpleTransfer(Medium.Blockchain),
                SimpleTransfer(Medium.Bank, null, mediumReceiveAccountId)
            )
    }
}

data class SimpleTransfer(
    /**
     * The [Medium] for this transfer.
     */
    val medium: Medium,
    /**
     * [Details] for this transfer. Will only ever be a [BlockchainDetails] object with a
     * receive address for the transfer out.
     */
    val details: BlockchainDetails? = null,
    /**
     * The ID of the bank account of the trader, which has previously been added to Coinify.
     * Required if the medium is bank.
     */
    val mediumReceiveAccountId: Int? = null
)