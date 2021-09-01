package piuk.blockchain.android.coincore.eth

import com.blockchain.annotations.CommonCode
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.isCustodialOnly
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.android.coincore.impl.BackendNotificationUpdater
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.coincore.impl.NotificationAddresses

internal class EthAsset(
    payloadManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    private val assetCatalogue: Lazy<AssetCatalogue>,
    custodialManager: CustodialWalletManager,
    interestBalances: InterestBalanceDataManager,
    tradingBalances: TradingBalanceDataManager,
    exchangeRates: ExchangeRatesDataManager,
    currencyPrefs: CurrencyPrefs,
    private val walletPrefs: WalletStatus,
    private val notificationUpdater: BackendNotificationUpdater,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    identity: UserIdentity,
    features: InternalFeatureFlagApi
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    currencyPrefs,
    labels,
    custodialManager,
    interestBalances,
    tradingBalances,
    pitLinking,
    crashLogger,
    identity,
    features
) {
    override val asset: AssetInfo
        get() = CryptoCurrency.ETHER

    override val isCustodialOnly: Boolean = asset.isCustodialOnly
    override val multiWallet: Boolean = false

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            assetCatalogue.value,
            labels.getDefaultNonCustodialWalletLabel()
        )

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
                    identity = identity,
                    assetCatalogue = assetCatalogue.value
                )
            }.doOnSuccess {
                updateBackendNotificationAddresses(it)
            }.map {
                listOf(it)
            }

    override fun loadCustodialAccounts(): Single<SingleAccountList> =
        Single.just(
            listOf(
                CustodialTradingAccount(
                    asset = asset,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    tradingBalances = tradingBalances,
                    identity = identity,
                    features = features
                )
            )
        )

    private fun updateBackendNotificationAddresses(account: EthCryptoWalletAccount) {
        val notify = NotificationAddresses(
            assetTicker = asset.ticker,
            addressList = listOf(account.address)
        )
        return notificationUpdater.updateNotificationBackend(notify)
    }

    @CommonCode("Exists in UsdtAsset and PaxAsset")
    override fun parseAddress(address: String, label: String?): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(extractAddress(address))).flatMapMaybe { isValid ->
            if (isValid) {
                ethDataManager.isContractAddress(extractAddress(address)).flatMapMaybe { isContract ->
                    if (isContract) {
                        throw AddressParseError(ETH_UNEXPECTED_CONTRACT_ADDRESS)
                    } else {
                        val normalisedAddress = address.removePrefix(FormatsUtil.ETHEREUM_PREFIX)
                        var parts = normalisedAddress.split("?")
                        val addressPart = parts.getOrNull(0)
                        if (parts.size > 1) {
                            parts = parts[1].split("&");
                        }

                        val amountPart = parts.find {
                            it.startsWith(ETH_ADDRESS_AMOUNT_PART, true)
                        }?.let {
                            CryptoValue.fromMinor(
                                CryptoCurrency.ETHER, it.removePrefix(ETH_ADDRESS_AMOUNT_PART).toBigDecimal()
                            )
                        }
                        Maybe.just(
                            EthAddress(address = addressPart!!, label = label ?: address, amount = amountPart)
                        )
                    }
                }
            } else {
                Maybe.empty()
            }
        }

    private fun extractAddress(address: String): String {
        val parts = address.removePrefix(FormatsUtil.ETHEREUM_PREFIX).split("?")
        return parts.getOrNull(0)!!
    }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)

    companion object {
        private const val ETH_ADDRESS_AMOUNT_PART = "value="
    }

}

internal class EthAddress(
    override val address: String,
    override val label: String = address,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    override val amount: CryptoValue? = null
) : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.ETHER
}
