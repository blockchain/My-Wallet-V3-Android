package piuk.blockchain.android.coincore

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.api.AddressMappingService
import com.blockchain.api.DomainAddressNotFound
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber
import java.lang.IllegalStateException

class AddressParseError(val error: Error) : Exception("Error Parsing address") {
    enum class Error {
        ETH_UNEXPECTED_CONTRACT_ADDRESS
    }
}

interface TransactionTarget {
    val label: String
    val onTxCompleted: (TxResult) -> Completable
        get() = { _ ->
            Completable.complete()
        }
}

// An invoice has a fixed amount
interface InvoiceTarget

interface ReceiveAddress : TransactionTarget {
    val address: String
}

object NullAddress : ReceiveAddress {
    override val label: String = ""
    override val address: String = ""
}

interface CryptoTarget : TransactionTarget {
    val asset: CryptoCurrency
    val memo: String?
        get() = null
}

interface CryptoAddress : CryptoTarget, ReceiveAddress {
    fun toUrl(amount: CryptoValue = CryptoValue.zero(asset)) = address
}

interface AddressFactory {
    fun parse(address: String): Single<Set<ReceiveAddress>>
    fun parse(address: String, ccy: CryptoCurrency): Maybe<ReceiveAddress>
}

class AddressFactoryImpl(
    private val coincore: Coincore,
    private val addressResolver: AddressMappingService,
    @Suppress("unused")
    private val features: InternalFeatureFlagApi
) : AddressFactory {

    /** Build the set of possible address for a given input string.
     * If the string is not a valid address for any available tokens, then return
     * an empty set
     **/
    override fun parse(address: String): Single<Set<ReceiveAddress>> =
        Maybe.merge(
            coincore.allAssets.map { asset ->
                asset.parseAddress(address)
                    .doOnError { Timber.e("**** ERROR: $asset") }
                    .onErrorComplete()
            }
        ).toList().map { it.toSet() }

    override fun parse(address: String, ccy: CryptoCurrency): Maybe<ReceiveAddress> =
        isDomainAddress(address)
            .flatMapMaybe { isDomain ->
                if (isDomain) {
                    resolveDomainAddress(address, ccy)
                } else {
                    coincore[ccy].parseAddress(address)
                }
            }

    private fun resolveDomainAddress(address: String, ccy: CryptoCurrency): Maybe<ReceiveAddress> =
        addressResolver.resolveAssetAddress(address, ccy.networkTicker)
            .flatMapMaybe { resolved ->
                if (resolved.isEmpty())
                    Maybe.empty()
                else
                    coincore[ccy].parseAddress(resolved, address)
            }.onErrorResumeNext(::handleResolutionApiError)

    private fun handleResolutionApiError(t: Throwable): Maybe<ReceiveAddress> =
        when (t) {
            is DomainAddressNotFound -> Maybe.empty()
            else -> {
                Timber.e(t, "Failed to resolve domain address")
                throw IllegalStateException(t)
            }
        }

    private fun isDomainAddress(address: String): Single<Boolean> =
        Single.just(address.contains('.'))
}