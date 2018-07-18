package piuk.blockchain.android.ui.home

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.TradeData
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
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

    fun getCompletedCoinifyTrades() =
        exchangeService.getExchangeMetaData()
            .flatMap(::pairExchangeDataWithTrades)
            .filter { (_, trade) ->
                !trade.isSellTransaction()
            }
            .filter { (_, trade) ->
                trade.state == TradeState.Completed
            }
            .map { (exchangeData, trade) ->
                Triple(
                    exchangeData,
                    trade,
                    tradeMetaData(exchangeData, trade)
                )
            }
            .filter { (_, _, metaData) ->
                metaData?.isConfirmed == false
            }
            .doOnNext { (exchangeData, _, metaData) ->
                (metaData ?: throw IllegalStateException("metaData is null but shouldn't be at this point"))
                    .isConfirmed = true
                updateMetadataEntry(exchangeData)
            }
            .map { (_, trade, _) ->
                (trade.transferOut.details as BlockchainDetails).eventData?.txId
                    ?: throw IllegalStateException("TxId is null but shouldn't be at this point")
            }

    private fun tradeMetaData(
        exchangeData: ExchangeData,
        trade: CoinifyTrade
    ): TradeData? {
        val tradeMetadata = exchangeData.coinify?.trades ?: emptyList()

        // Check if unconfirmed in metadata
        val metadata = tradeMetadata.firstOrNull { it.id == trade.id }
        return metadata
    }

    private fun pairExchangeDataWithTrades(exchangeData: ExchangeData): Observable<Pair<ExchangeData, CoinifyTrade>> {
        return exchangeData.coinify?.token?.let {
            coinifyDataManager.getTrades(it)
                .map { exchangeData to it }
        } ?: Observable.empty()
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