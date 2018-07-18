package piuk.blockchain.android.ui.home

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString

class CoinifyTradeCompleteListener(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager,
    private val metadataManager: MetadataManager
) {

    fun getCompletedCoinifyTrades(): Observable<String> =
        exchangeService.getExchangeMetaData()
            .flatMap { exchangeData ->
                exchangeData.coinify?.token?.let {
                    coinifyDataManager.getTrades(it)
                        .map { exchangeData to it }
                } ?: Observable.empty()
            }
            .flatMap {
                val exchangeData = it.first
                val trade = it.second
                val tradeMetadata = exchangeData.coinify?.trades ?: emptyList()
                // Only buy transactions
                if (trade.isSellTransaction()) {
                    return@flatMap Observable.empty<String>()
                }

                if (trade.state === TradeState.Completed) {
                    // Check if unconfirmed in metadata
                    val metadata = tradeMetadata.firstOrNull { it.id == trade.id }
                    if (metadata?.isConfirmed == false) {
                        // Update object to confirmed
                        metadata.isConfirmed = true
                        // Update metadata entry in the background
                        updateMetadataEntry(exchangeData)
                        // Return transaction hash
                        return@flatMap Observable.just(
                            (trade.transferOut.details as BlockchainDetails).eventData?.txId
                                ?: throw IllegalStateException("TxId is null but shouldn't be at this point")
                        )
                    }
                }
                return@flatMap Observable.empty<String>()
            }

    private fun updateMetadataEntry(exchangeData: ExchangeData) {
        metadataManager.saveToMetadata(
            exchangeData.toSerialisedString(),
            ExchangeService.METADATA_TYPE_EXCHANGE
        ).subscribeOn(Schedulers.io())
            // Not a big problem if updating this record fails here
            .emptySubscribe()
    }
}