package piuk.blockchain.android.coincore.btc

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
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
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager

internal class BtcAsset(
    payloadManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeDataManager: FeeDataManager,
    private val coinsWebsocket: CoinsWebSocketStrategy,
    custodialManager: CustodialWalletManager,
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
        get() = CryptoCurrency.BTC

    override fun initToken(): Completable =
        Completable.complete()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            with(payloadManager) {
                val result = mutableListOf<CryptoAccount>()
                accounts.forEachIndexed { i, account ->
                    val btcAccount = btcAccountFromPayloadAccount(i, account)
                    if (btcAccount.isDefault) {
                        updateOfflineCache(btcAccount)
                    }
                    result.add(btcAccount)
                }

                importedAddresses.forEach { account ->
                    result.add(btcAccountFromImportedAccount(account))
                }
                result
            }
        }

    private fun updateOfflineCache(account: BtcCryptoWalletAccount) {
        require(account.isDefault)
        require(!account.isArchived)

        return offlineAccounts.updateOfflineAddresses(
            Single.fromCallable {
                val result = mutableListOf<CachedAddress>()

                for (i in 0 until OFFLINE_CACHE_ITEM_COUNT) {
                    account.getReceiveAddressAtPosition(i)?.let {
                        result += CachedAddress(
                            address = it,
                            addressUri = FormatsUtil.toDisambiguatedBtcAddress(it)
                        )
                    }
                }
                BtcOfflineAccountItem(
                    accountLabel = account.label,
                    addressList = result
                )
            }
        )
    }

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            val amountField = "amount="
            val normalisedAddress = address.removePrefix(FormatsUtil.BTC_PREFIX)
            val parts = normalisedAddress.split("?")
            val addressPart = parts.getOrNull(0)
            val amountPart = parts.find {
                it.startsWith(amountField, true)
            }?.let {
                CryptoValue.fromMajor(CryptoCurrency.BTC, it.substring(amountField.length).toBigDecimal())
            }
            if (addressPart != null && isValidAddress(addressPart)) {
                BtcAddress(address = addressPart, label = label ?: address, amount = amountPart)
            } else {
                null
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidBitcoinAddress(address)

    fun createAccount(label: String, secondPassword: String?): Single<BtcCryptoWalletAccount> =
        payloadManager.createNewAccount(label, secondPassword)
            .singleOrError()
            .map { btcAccountFromPayloadAccount(payloadManager.accountCount - 1, it) }
            .doOnSuccess { forceAccountsRefresh() }
            .doOnSuccess { coinsWebsocket.subscribeToXpubBtc(it.xpubAddress) }

    fun importAddressFromKey(
        keyData: String,
        keyFormat: String,
        keyPassword: String? = null, // Required for BIP38 format keys
        walletSecondPassword: String? = null
    ): Single<BtcCryptoWalletAccount> {
        require(keyData.isNotEmpty())
        require(keyPassword != null || keyFormat != PrivateKeyFactory.BIP38)

        return when (keyFormat) {
            PrivateKeyFactory.BIP38 -> extractBip38Key(keyData, keyPassword!!)
            else -> extractKey(keyData, keyFormat)
        }.map { key ->
            if (!key.hasPrivKey)
                throw Exception()
            key
        }.flatMap { key ->
            payloadManager.addImportedAddressFromKey(key, walletSecondPassword)
        }.map { importedAddress ->
            btcAccountFromImportedAccount(importedAddress)
        }.doOnSuccess {
            forceAccountsRefresh()
        }.doOnSuccess { btcAccount ->
            coinsWebsocket.subscribeToExtraBtcAddress(btcAccount.xpubAddress)
        }
    }

    private fun extractBip38Key(keyData: String, keyPassword: String): Single<SigningKey> =
        payloadManager.getBip38KeyFromImportedData(keyData, keyPassword)

    private fun extractKey(keyData: String, keyFormat: String): Single<SigningKey> =
        payloadManager.getKeyFromImportedData(keyFormat, keyData)

    private fun btcAccountFromPayloadAccount(index: Int, payloadAccount: Account): BtcCryptoWalletAccount =
        BtcCryptoWalletAccount.createHdAccount(
            jsonAccount = payloadAccount,
            payloadManager = payloadManager,
            hdAccountIndex = index,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialManager,
            refreshTrigger = this,
            identity = identity
        )

    private fun btcAccountFromImportedAccount(payloadAccount: ImportedAddress): BtcCryptoWalletAccount =
        BtcCryptoWalletAccount.createImportedAccount(
            importedAccount = payloadAccount,
            payloadManager = payloadManager,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialManager,
            refreshTrigger = this,
            identity = identity
        )

    companion object {
        private const val OFFLINE_CACHE_ITEM_COUNT = 5
    }
}

internal class BtcAddress(
    override val address: String,
    override val label: String = address,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    private val amount: CryptoValue? = null
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.BTC

    override fun toUrl(amount: CryptoValue): String {
        return FormatsUtil.toBtcUri(address, amount.toBigInteger())
    }
}
