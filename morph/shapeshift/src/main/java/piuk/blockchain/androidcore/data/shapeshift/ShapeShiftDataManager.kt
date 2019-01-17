package piuk.blockchain.androidcore.data.shapeshift

import com.blockchain.utils.Optional
import info.blockchain.wallet.shapeshift.ShapeShiftApi
import info.blockchain.wallet.shapeshift.ShapeShiftTrades
import info.blockchain.wallet.shapeshift.data.State
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.data.shapeshift.datastore.ShapeShiftDataStore
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import piuk.blockchain.androidcore.utils.extensions.applySchedulers

class ShapeShiftDataManager(
    private val shapeShiftApi: ShapeShiftApi,
    private val shapeShiftDataStore: ShapeShiftDataStore,
    private val metadataManager: MetadataManager,
    rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    private val tradeData
        get() = shapeShiftDataStore.tradeData ?: ShapeShiftTrades()

    /**
     * Must be called to initialize the ShapeShift trade metadata information.
     *
     * @return A [Completable] object
     */
    fun initShapeshiftTradeData(): Completable =
        rxPinning.call {
            fetchOrCreateShapeShiftTradeData()
                .flatMapCompletable {
                    shapeShiftDataStore.tradeData = it.first
                    if (it.second) {
                        save()
                    } else {
                        Completable.complete()
                    }
                }.subscribeOn(Schedulers.io())
        }

    /**
     * Clears all data in the [ShapeShiftDataStore]
     */
    fun clearShapeShiftData() = shapeShiftDataStore.clearData()

    /**
     * Returns the US state that the user has selected and stored in metadata, contained within an
     * [Optional] wrapper. If [Optional.None], no State has been set. Will throw an
     * [IllegalArgumentException] if [ShapeShiftTrades] has not been initialized.
     *
     * @return An [Observable] containing an [Optional]
     */
    fun getState(): Observable<Optional<State>> {
        tradeData.run {
            return when (usState) {
                null -> Observable.just(Optional.None)
                else -> Observable.just(Optional.Some(usState))
            }
        }
    }

    /**
     * Sets the user's selected State and saves it to metadata. Can be null to clear the saved
     * State. Will throw an [IllegalArgumentException] if [ShapeShiftTrades] has not been
     * initialized.
     *
     * @return A [Completable] object
     */
    fun setState(state: State?): Completable {
        tradeData.usState = state
        return save()
    }

    /**
     * Returns a list of [Trade] objects previously fetched from metadata. Note that this does
     * not refresh the list. Will throw an [IllegalArgumentException] if [ShapeShiftTrades] has
     * not been initialized.
     *
     * @return An [Observable] wrapping a list of [Trade] objects
     */
    fun getTradesList(): Observable<List<Trade>> {
        tradeData.run { return Observable.just(trades) }
    }

    /**
     * Returns a [Trade] object if found in the current list of [Trade] objects pulled from
     * metadata. Will throw an [IllegalArgumentException] if [ShapeShiftTrades] has not been
     * initialized.
     *
     * @param depositAddress The deposit address of the [Trade] you wish to find
     * @return A [Single] wrapping a [Trade]
     */
    fun findTrade(depositAddress: String): Single<Trade> {
        tradeData.run {
            val foundTrade = trades.firstOrNull { it.quote?.deposit == depositAddress }
            return if (foundTrade == null) {
                Single.error(Throwable("Trade not found"))
            } else {
                Single.just(foundTrade)
            }
        }
    }

    /**
     * Adds a new [Trade] object to the list of Trades and then saves it to the metadata service.
     * Will revert the status of the Trades list if the call fails. Will throw an
     * [IllegalArgumentException] if [ShapeShiftTrades] has not been initialized.
     *
     * @param trade The [Trade] object to be added to the list of Trades
     * @return A [Completable] object
     */
    fun addTradeToList(trade: Trade): Completable {
        tradeData.run {
            trades.add(trade)
            return save()
                // Reset state on failure
                .doOnError { trades.remove(trade) }
        }
    }

    /**
     * For development purposes only! Clears all [Trade] objects from the user's metadata and
     * stores an empty list instead. Will throw an [IllegalArgumentException] if [ShapeShiftTrades]
     * has not been initialized.
     *
     * @return A [Completable] object
     */
    fun clearAllTrades(): Completable {
        tradeData.run {
            trades?.clear()
            return save()
        }
    }

    /**
     * Takes a [Trade] object, replaces the current version of it stored in metadata and then saves
     * it. Will return an error if the [Trade] is not found. Will throw an
     * [IllegalArgumentException] if [ShapeShiftTrades] has not been initialized.
     *
     * @param trade The [Trade] object to be updated
     * @return A [Completable] object
     */
    fun updateTrade(trade: Trade): Completable {
        return tradeData.run {
            val foundTrade = findTradeByOrderId(trade.quote?.orderId)
            if (foundTrade == null) {
                Completable.error(Throwable("Trade not found"))
            } else {
                trades.remove(foundTrade)
                trades.add(trade)
                save()
                    // Reset state on failure
                    .doOnError {
                        trades.remove(trade)
                        trades.add(foundTrade)
                    }
            }
        }
    }

    fun findTradeByOrderId(orderId: String?): Trade? {
        return tradeData.run {
            trades.find { it.quote?.orderId == orderId }
        }
    }

    /**
     * Gets the [TradeStatusResponse] for a given [Trade] deposit address. Note that this won't
     * return an invalid [TradeStatusResponse] if the server returned an error response: it will
     * fail instead.
     *
     * @param depositAddress The [Trade] deposit address
     * @return An [Observable] wrapping a [TradeStatusResponse] object.
     */
    fun getTradeStatus(depositAddress: String?): Observable<TradeStatusResponse> {
        if (depositAddress.isNullOrBlank()) {
            return Observable.error(Throwable("null or blank address"))
        }
        return rxPinning.call<TradeStatusResponse> {
            shapeShiftApi.getTradeStatus(depositAddress)
                .flatMap {
                    if (it.error != null && it.status == null) {
                        Observable.error(Throwable(it.error))
                    } else {
                        Observable.just(it)
                    }
                }
        }.applySchedulers()
    }

    /**
     * Fetches the current trade metadata from the web, or else creates a new metadata entry
     * containing an empty list of [Trade] objects.
     *
     * @return A [ShapeShiftTrades] object wrapping trades functionality
     * @throws Exception Can throw various exceptions if the key is incorrect, the server is down
     * etc
     */
    @WebRequest
    @Throws(Exception::class)
    private fun fetchOrCreateShapeShiftTradeData(): Observable<Pair<ShapeShiftTrades, Boolean>> =
        metadataManager.fetchMetadata(ShapeShiftTrades.METADATA_TYPE_EXTERNAL)
            .map { optional ->

                val json = optional.orNull()
                var shapeShiftData = ShapeShiftTrades.load(json)
                var needsSave = false

                if (shapeShiftData == null) {
                    shapeShiftData = ShapeShiftTrades()
                    needsSave = true
                }

                Pair(shapeShiftData, needsSave)
            }

    fun save(): Completable {
        tradeData.run {
            return rxPinning.call {
                metadataManager.saveToMetadata(
                    shapeShiftDataStore.tradeData!!.toJson(),
                    ShapeShiftTrades.METADATA_TYPE_EXTERNAL
                )
            }.applySchedulers()
        }
    }

    data class TradeStatusPair(
        val tradeMetadata: Trade,
        val tradeStatusResponse: TradeStatusResponse
    )
}