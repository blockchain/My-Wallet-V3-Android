package piuk.blockchain.android.coincore

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.toAccountReference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.android.coincore.model.ActivitySummaryList
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalArgumentException
import java.math.BigInteger

class ETHTokens(
    private val ethDataManager: EthDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ChartsDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val crashLogger: CrashLogger,
    private val custodialWalletManager: CustodialWalletManager,
    rxBus: RxBus
) : AssetTokensBase(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun defaultAccount(): Single<AccountReference> =
        Single.just(getDefaultEthAccountRef())

    override fun receiveAddress(): Single<String> =
        Single.just(getDefaultEthAccountRef().receiveAddress)

    private fun getDefaultEthAccountRef(): AccountReference =
        ethDataManager.getEthWallet()?.account?.toAccountReference()
            ?: throw Exception("No ether wallet found")

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.ETHER)

    override fun noncustodialBalance(): Single<CryptoValue> =
        etheriumWalletInitialiser()
            .andThen(ethDataManager.fetchEthAddress())
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        val ref = account as? AccountReference.Ethereum
            ?: throw IllegalArgumentException("Not an XLM Account Ref")

        return etheriumWalletInitialiser()
            .andThen(ethDataManager.getBalance(ref.address))
            .map { CryptoValue.etherFromWei(it) }
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.ETHER, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(
            CryptoCurrency.ETHER,
            currencyPrefs.selectedFiatCurrency,
            epochWhen
        )

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(
            CryptoCurrency.ETHER,
            currencyPrefs.selectedFiatCurrency,
            period
        )

    private var isWalletUninitialised = true

    private fun etheriumWalletInitialiser() =
        if (isWalletUninitialised) {
            ethDataManager.initEthereumWallet(
                stringUtils.getString(R.string.eth_default_account_label),
                stringUtils.getString(R.string.pax_default_account_label)
            ).doOnError { throwable ->
                crashLogger.logException(throwable, "Failed to load ETH wallet")
            }.doOnComplete {
                isWalletUninitialised = false
            }
        } else {
            Completable.complete()
        }

    override fun onLogoutSignal(event: AuthEvent) {
        isWalletUninitialised = true
        ethDataManager.clearEthAccountDetails()
    }

    // Activity/transactions moved over from TransactionDataListManager.
    // TODO Requires some reworking, but that can happen later. After the code & tests are moved and working.
    override fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        getTransactions()
            .singleOrError()

    private fun getTransactions(): Observable<ActivitySummaryList> =
        ethDataManager.getLatestBlock()
            .flatMapSingle { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map {
                        val ethFeeForPaxTransaction = it.to.equals(
                            ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                            ignoreCase = true
                        )
                        EthActivitySummaryItem(
                            ethDataManager.getEthResponseModel()!!,
                            it,
                            ethFeeForPaxTransaction,
                            latestBlock.blockHeight,
                            exchangeRates,
                            currencyPrefs.selectedFiatCurrency
                        )
                    }.toList()
                }
}

private class EthActivitySummaryItem(
    private val combinedEthModel: CombinedEthModel,
    private val ethTransaction: EthTransaction,
    override val isFeeTransaction: Boolean,
    private val blockHeight: Long,
    exchangeRates: ExchangeRateDataManager,
    selectedFiat: String
) : ActivitySummaryItem() {

    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.ETHER

    override val direction: TransactionSummary.Direction by unsafeLazy {
        combinedEthModel.getAccounts().let {
            when {
                it[0] == ethTransaction.to && it[0] == ethTransaction.from ->
                    TransactionSummary.Direction.TRANSFERRED
                it.contains(ethTransaction.from) ->
                    TransactionSummary.Direction.SENT
                else ->
                    TransactionSummary.Direction.RECEIVED
            }
        }
    }

    override val timeStamp: Long
        get() = ethTransaction.timeStamp

    override val totalCrypto: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.ETHER,
            when (direction) {
                TransactionSummary.Direction.RECEIVED -> ethTransaction.value
                else -> ethTransaction.value.plus(ethTransaction.gasUsed.multiply(ethTransaction.gasPrice))
            }
        )
    }

    override val totalFiat: FiatValue by unsafeLazy {
        totalCrypto.toFiat(exchangeRates, selectedFiat)
    }

    override val fee: Observable<BigInteger>
        get() = Observable.just(ethTransaction.gasUsed.multiply(ethTransaction.gasPrice))

    override val hash: String
        get() = ethTransaction.hash

    override val inputsMap: Map<String, BigInteger>
        get() = mapOf(ethTransaction.from to ethTransaction.value)

    override val outputsMap: Map<String, BigInteger>
        get() = mapOf(ethTransaction.to to ethTransaction.value)

    override val confirmations: Int
        get() {
            val blockNumber = ethTransaction.blockNumber ?: return 0
            val blockHash = ethTransaction.blockHash ?: return 0

            return if (blockNumber == 0L || blockHash == "0x") 0 else (blockHeight - blockNumber).toInt()
        }
}
