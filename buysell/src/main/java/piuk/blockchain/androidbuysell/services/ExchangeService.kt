package piuk.blockchain.androidbuysell.services

import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.ReplaySubject
import org.bitcoinj.crypto.DeterministicKey
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.TradeData
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.websockets.WebSocketReceiveEvent
import piuk.blockchain.androidcore.utils.annotations.Mockable
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.toKotlinObject
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by justin on 5/1/17.
 */

@Mockable
@Singleton
class ExchangeService @Inject constructor(
        private val payloadManager: PayloadManager,
        private val rxBus: RxBus
) {

    private var metadataSubject: ReplaySubject<Metadata> = ReplaySubject.create(1)
    private var didStartLoad: Boolean = false

    fun getWebViewLoginDetails(): Observable<WebViewLoginDetails> = Observable.zip(
            getExchangeData().flatMap { buyMetadata ->
                Observable.fromCallable {
                    val metadata = buyMetadata.metadata
                    metadata ?: ""
                }.applySchedulers()
            },
            getExchangeData().flatMap { buyMetadata ->
                Observable.fromCallable {
                    buyMetadata.fetchMagic()
                    val magicHash = buyMetadata.magicHash
                    if (magicHash == null) "" else Hex.toHexString(magicHash)
                }.applySchedulers()
            },
            BiFunction { externalJson, magicHash ->
                val walletJson = payloadManager.payload!!.toJson()
                val password = payloadManager.tempPassword
                WebViewLoginDetails(walletJson, password, externalJson, magicHash)
            }
    )

    private fun getExchangeData(): Observable<Metadata> {
        if (!didStartLoad) {
            reloadExchangeData()
            didStartLoad = true
        }
        return metadataSubject
    }

    private fun getPendingTradeAddresses(): Observable<String> = getExchangeData()
            .flatMap { metadata ->
                Observable.fromCallable {
                    val exchangeData = metadata.metadata
                    exchangeData ?: ""
                }.applySchedulers()
            }
            .flatMapIterable { exchangeData ->
                if (exchangeData.isEmpty()) {
                    emptyList<TradeData>()
                } else {
                    val data = exchangeData.toKotlinObject<ExchangeData>()
                    val trades = ArrayList<TradeData>()
                    when {
                        data.coinify != null -> trades.addAll(data.coinify?.trades ?: arrayListOf())
                        data.sfox != null -> trades.addAll(data.sfox?.trades ?: arrayListOf())
                        data.unocoin != null -> trades.addAll(data.unocoin?.trades ?: arrayListOf())
                    }

                    trades
                }
            }
            .filter { tradeData -> tradeData.isBuy && !tradeData.isConfirmed }
            .map { tradeData ->
                payloadManager.getReceiveAddressAtArbitraryPosition(
                        payloadManager.payload!!.hdWallets[0].getAccount(tradeData.accountIndex),
                        tradeData.receiveIndex
                )!!
            }
            .distinct()

    fun getExchangeMetaData(): Observable<ExchangeData> = getExchangeData()
            .flatMap { metadata ->
                Observable.fromCallable {
                    val exchangeData = metadata.metadata
                    exchangeData ?: ""
                }.applySchedulers()
            }
            .map { exchangeData ->
                if (exchangeData.isEmpty()) {
                    ExchangeData()
                } else {
                    exchangeData.toKotlinObject()
                }
            }

    fun wipe() {
        metadataSubject = ReplaySubject.create(1)
        didStartLoad = false
    }

    fun watchPendingTrades(): Observable<String> {
        val receiveEvents = rxBus.register(WebSocketReceiveEvent::class.java)

        return getPendingTradeAddresses()
                .doOnNext { Timber.d("watchPendingTrades: watching receive address: %s", it) }
                .flatMap { address ->
                    receiveEvents
                            .filter { event -> event.address == address }
                            .map { it.hash }
                }
    }

    fun reloadExchangeData() {
        val metadataNodeFactory = payloadManager.metadataNodeFactory

        if (metadataNodeFactory != null) {
            val metadataNode = metadataNodeFactory.metadataNode

            if (metadataNode != null) {
                val exchangeDataStream = getMetadata(metadataNode)
                exchangeDataStream.subscribeWith(metadataSubject)
            } else {
                Timber.e("MetadataNode not generated yet. Wallet possibly double encrypted.")
            }
        } else {
            //PayloadManager likely to have been cleared at this point.
            //TODO This avoids high velocity crash. A proper analyses why this happens would be beneficial.
            Timber.e("ExchangeService.reloadExchangeData - MetadataNodeFactory is null.")
        }
    }

    private fun getMetadata(metadataHDNode: DeterministicKey): Observable<Metadata> =
            Observable.fromCallable {
                Metadata.Builder(metadataHDNode, METADATA_TYPE_EXCHANGE).build()
            }.applySchedulers()

    companion object {

        const val METADATA_TYPE_EXCHANGE = 3

    }

}
