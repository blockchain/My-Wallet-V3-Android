package piuk.blockchain.android.coincore.xlm

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.XlmAccountReference
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class XlmCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    private var xlmAccountReference: XlmAccountReference,
    private val xlmManager: XlmDataManager,
    override val exchangeRates: ExchangeRatesDataManager,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val walletPreferences: WalletStatus,
    private val custodialWalletManager: CustodialWalletManager,
    identity: UserIdentity
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.XLM, custodialWalletManager, identity) {

    override val baseActions: Set<AssetAction> = defaultActions
    override val isDefault: Boolean = true // Only one account ever, so always default

    override val label: String
        get() = xlmAccountReference.label

    internal val address: String
        get() = xlmAccountReference.accountId

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = xlmManager.getBalance()
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.zero(asset))
            }
            .map { it }

    override val actionableBalance: Single<Money>
        get() = xlmManager.getBalanceAndMin().map {
            it.balance - it.minimumBalance
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            XlmAddress(_address = address, _label = label)
        )

    override val activity: Single<ActivitySummaryList>
        get() = xlmManager.getTransactionList()
            .onErrorResumeNext { Single.just(emptyList()) }
            .mapList {
                XlmActivitySummaryItem(
                    it,
                    exchangeRates,
                    account = this,
                    payloadDataManager
                )
            }.flatMap {
                appendTradeActivity(custodialWalletManager, asset, it)
            }.doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        if (newLabel == label) return Completable.complete()
        val revertLabel = label
        xlmAccountReference = xlmAccountReference.copy(label = newLabel)
        return xlmManager.updateAccountLabel(newLabel)
            .doOnError { xlmAccountReference = xlmAccountReference.copy(label = revertLabel) }
    }

    override fun createTxEngine(target: TransactionTarget): TxEngine =
        XlmOnChainTxEngine(
            xlmDataManager = xlmManager,
            xlmFeesFetcher = xlmFeesFetcher,
            walletOptionsDataManager = walletOptionsDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences
        )
}
