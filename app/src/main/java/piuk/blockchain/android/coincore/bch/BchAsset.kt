package piuk.blockchain.android.coincore.bch

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.bch.CashAddress
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.CachedAddress
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.coincore.impl.OfflineAccountUpdater
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import timber.log.Timber

private const val BCH_URL_PREFIX = "bitcoincash:"

internal class BchAsset(
    payloadManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    custodialManager: CustodialWalletManager,
    private val feeDataManager: FeeDataManager,
    private val sendDataManager: SendDataManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    private val walletPreferences: WalletStatus,
    offlineAccounts: OfflineAccountUpdater,
    identity: UserIdentity,
    features: InternalFeatureFlagApi
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    offlineAccounts,
    identity,
    features
) {
    override val asset: CryptoCurrency
        get() = CryptoCurrency.BCH

    override fun initToken(): Completable =
        bchDataManager.initBchWallet(labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.BCH))
            .doOnError { Timber.e("Unable to init BCH, because: $it") }
            .onErrorComplete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            with(bchDataManager) {
                mutableListOf<CryptoAccount>().apply {
                    getAccountMetadataList().forEachIndexed { i, account ->
                        val bchAccount = BchCryptoWalletAccount.createBchAccount(
                            payloadManager = payloadManager,
                            jsonAccount = account,
                            bchManager = bchDataManager,
                            addressIndex = i,
                            exchangeRates = exchangeRates,
                            feeDataManager = feeDataManager,
                            sendDataManager = sendDataManager,
                            walletPreferences = walletPreferences,
                            custodialWalletManager = custodialManager,
                            refreshTrigger = this@BchAsset,
                            identity = identity
                        )
                        if (bchAccount.isDefault) {
                            updateOfflineCache(bchAccount)
                        }
                        add(bchAccount)
                    }
                }
            }
        }

    private fun updateOfflineCache(account: BchCryptoWalletAccount) {
        require(account.isDefault)
        require(!account.isArchived)

        return offlineAccounts.updateOfflineAddresses(
            Single.fromCallable {
                val result = mutableListOf<CachedAddress>()

                for (i in 0 until OFFLINE_CACHE_ITEM_COUNT) {
                    account.getReceiveAddressAtPosition(i)?.let {
                        val bech32 = CashAddress.toBech32Url(it)
                        result += CachedAddress(
                            address = it,
                            addressUri = bech32
                        )
                    }
                }
                BchOfflineAccountItem(
                    accountLabel = account.label,
                    addressList = result
                )
            }
        )
    }

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            val normalisedAddress = address.removePrefix(BCH_URL_PREFIX)
            if (isValidAddress(normalisedAddress)) {
                BchAddress(normalisedAddress, label ?: address)
            } else {
                null
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidBCHAddress(address)

    fun createAccount(xpub: String): Completable {
        bchDataManager.createAccount(xpub)
        return bchDataManager.syncWithServer().doOnComplete { forceAccountsRefresh() }
    }

    companion object {
        private const val OFFLINE_CACHE_ITEM_COUNT = 5
    }
}

internal class BchAddress(
    address_: String,
    override val label: String = address_,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() }
) : CryptoAddress {
    override val address: String = address_.removeBchUri()
    override val asset: CryptoCurrency = CryptoCurrency.BCH

    override fun toUrl(amount: CryptoValue): String {
        return "$BCH_URL_PREFIX$address"
    }
}

private fun String.removeBchUri(): String = this.replace(BCH_URL_PREFIX, "")
