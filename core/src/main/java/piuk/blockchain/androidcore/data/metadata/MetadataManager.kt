package piuk.blockchain.androidcore.data.metadata

import com.google.common.base.Optional
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.metadata.Saveable
import io.reactivex.Completable
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.MetadataUtils
import piuk.blockchain.androidcore.utils.annotations.Mockable
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import javax.inject.Inject

/**
 * Manages metadata nodes/keys derived from a user's wallet credentials.
 * This helps to avoid repeatedly asking user for second password.
 *
 * There are currently 2 nodes/keys (serialized privB58):
 * sharedMetadataNode   - used for inter-wallet communication
 * metadataNode         - used for storage
 *
 * The above nodes/keys can be derived from a user's master private key.
 * After these keys have been derived we store them on the metadata service with a node/key
 * derived from 'guid + sharedkey + wallet password'. This will allow us to retrieve these derived
 * keys with just a user's credentials and not derive them again.
 *
 */
@Mockable
@PresenterScope
class MetadataManager @Inject constructor(
    private val payloadDataManager: PayloadDataManager,
    private val metadataUtils: MetadataUtils,
    rxBus: RxBus
) {
    private val rxPinning = RxPinning(rxBus)

    fun attemptMetadataSetup() = initMetadataNodesObservable()

    fun decryptAndSetupMetadata(secondPassword: String): Completable {
        payloadDataManager.decryptHDWallet(secondPassword)
        return payloadDataManager.generateNodes()
            .andThen(initMetadataNodesObservable())
    }

    fun fetchMetadata(metadataType: Int): Observable<Optional<String>> =
        rxPinning.call<Optional<String>> {
            payloadDataManager.getMetadataNodeFactory().map { nodeFactory ->
                metadataUtils.getMetadataNode(nodeFactory.metadataNode, metadataType)
                    .metadataOptional
            }
        }.applySchedulers()

    fun saveToMetadata(data: String, metadataType: Int): Completable = rxPinning.call {
        payloadDataManager.getMetadataNodeFactory().flatMapCompletable {
            Completable.fromCallable {
                metadataUtils.getMetadataNode(it.metadataNode, metadataType).putMetadata(data)
            }
        }.applySchedulers()
    }

    fun saveToMetadata(saveable: Saveable): Completable = rxPinning.call {
        payloadDataManager.getMetadataNodeFactory().flatMapCompletable {
            Completable.fromCallable {
                metadataUtils.getMetadataNode(it.metadataNode, saveable.metadataType)
                    .putMetadata(saveable.toJson())
            }
        }.applySchedulers()
    }

    /**
     * Loads or derives the stored nodes/keys from the metadata service.
     *
     * @throws InvalidCredentialsException If nodes/keys cannot be derived because wallet is double encrypted
     */
    private fun initMetadataNodesObservable(): Completable = rxPinning.call {
        payloadDataManager.loadNodes()
            .map { loaded ->
                if (!loaded) {
                    if (payloadDataManager.isDoubleEncrypted) {
                        throw InvalidCredentialsException("Unable to derive metadata keys, payload is double encrypted")
                    } else {
                        true
                    }
                } else {
                    false
                }
            }
            .flatMap { needsGeneration ->
                if (needsGeneration) {
                    payloadDataManager.generateAndReturnNodes()
                } else {
                    payloadDataManager.getMetadataNodeFactory()
                }
            }.flatMapCompletable { Completable.complete() }
    }.applySchedulers()
}