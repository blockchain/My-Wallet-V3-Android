package piuk.blockchain.android.coincore.eth

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxSourceState
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.util.concurrent.atomic.AtomicBoolean

internal class EthCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    private val jsonAccount: EthereumAccount,
    private val ethDataManager: EthDataManager,
    private val fees: FeeDataManager,
    private val walletPreferences: WalletStatus,
    override val exchangeRates: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val assetCatalogue: AssetCatalogue,
    identity: UserIdentity
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.ETHER, custodialWalletManager, identity) {

    override val baseActions: Set<AssetAction> = defaultActions

    internal val address: String
        get() = jsonAccount.address
    override val label: String
        get() = jsonAccount.label

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.zero(CryptoCurrency.ETHER))
            }
            .map { it }

    override val actionableBalance: Single<Money>
        get() = accountBalance

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            EthAddress(
                address = address,
                label = label
            )
        )

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        val revertLabel = label
        jsonAccount.label = newLabel
        return ethDataManager.updateAccountLabel(newLabel)
            .doOnError { jsonAccount.label = revertLabel }
    }

    override val activity: Single<ActivitySummaryList>
        get() = ethDataManager.getLatestBlockNumber()
            .flatMap { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map { list ->
                        list.map { transaction ->
                            val isEr20FeeTransaction = isErc20FeeTransaction(transaction.to)
                            EthActivitySummaryItem(
                                ethDataManager,
                                transaction,
                                isEr20FeeTransaction,
                                latestBlock.number.toLong(),
                                exchangeRates,
                                account = this
                            )
                        }
                    }
                    .flatMap {
                        appendTradeActivity(custodialWalletManager, asset, it)
                    }
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    fun isErc20FeeTransaction(to: String): Boolean =
        assetCatalogue.supportedL2Assets(asset).firstOrNull {
            to.equals(ethDataManager.getErc20TokenData(it).contractAddress, true)
        } != null

    override val isDefault: Boolean = true // Only one ETH account, so always default

    override val sourceState: Single<TxSourceState>
        get() = super.sourceState.flatMap { state ->
            ethDataManager.isLastTxPending().map { hasUnconfirmed ->
                if (hasUnconfirmed) {
                    TxSourceState.TRANSACTION_IN_FLIGHT
                } else {
                    state
                }
            }
        }

    override fun createTxEngine(target: TransactionTarget): TxEngine =
        EthOnChainTxEngine(
            ethDataManager = ethDataManager,
            feeManager = fees,
            requireSecondPassword = ethDataManager.requireSecondPassword,
            walletPreferences = walletPreferences
        )
}
