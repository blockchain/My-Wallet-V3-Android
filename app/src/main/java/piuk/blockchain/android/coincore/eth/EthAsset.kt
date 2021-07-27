package piuk.blockchain.android.coincore.eth

import com.blockchain.annotations.CommonCode
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CachedAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.android.coincore.SimpleOfflineCacheItem
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.impl.OfflineAccountUpdater
import piuk.blockchain.android.identity.UserIdentity

internal class EthAsset(
    payloadManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    private val walletPrefs: WalletStatus,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    identity: UserIdentity,
    offlineAccounts: OfflineAccountUpdater,
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

    private val labelList = mapOf(
        CryptoCurrency.ETHER to labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.ETHER),
        CryptoCurrency.PAX to labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.PAX),
        CryptoCurrency.USDT to labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.USDT),
        CryptoCurrency.DGLD to labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.DGLD),
        CryptoCurrency.AAVE to labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.AAVE),
        CryptoCurrency.YFI to labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.YFI)
    )

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(labelList)

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(ethDataManager.getEthWallet() ?: throw Exception("No ether wallet found"))
            .map {
                EthCryptoWalletAccount(
                    payloadManager = payloadManager,
                    ethDataManager = ethDataManager,
                    fees = feeDataManager,
                    jsonAccount = it.account,
                    walletPreferences = walletPrefs,
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    identity = identity
                )
            }.doOnSuccess {
                updateOfflineCache(it)
            }.map {
                listOf(it)
            }

    private fun updateOfflineCache(account: EthCryptoWalletAccount) {
        offlineAccounts.updateOfflineAddresses(
            Single.just(
                SimpleOfflineCacheItem(
                    networkTicker = CryptoCurrency.ETHER.networkTicker,
                    accountLabel = account.label,
                    address = CachedAddress(
                        account.address,
                        account.address
                    )
                )
            )
        )
    }

    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            val amountField = "value="
            val normalisedAddress = address.removePrefix(FormatsUtil.ETHEREUM_PREFIX)
            var parts = normalisedAddress.split("?")
            val addressPart = parts.getOrNull(0)

            if (parts.size > 1) {
                parts = parts[1].split("&");
            }

            val amountPart = parts.find {
                it.startsWith(amountField, true)
            }?.let {
                CryptoValue.fromMinor(CryptoCurrency.ETHER, it.substring(amountField.length).toBigDecimal())
            }
            if (addressPart != null && isValidAddress(addressPart)) {
                EthAddress(address = addressPart, label = label ?: address, amount = amountPart)
            } else {
                null
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class EthAddress(
    override val address: String,
    override val label: String = address,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    private val amount: CryptoValue? = null
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.ETHER
}
