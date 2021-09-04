package piuk.blockchain.android.coincore

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import info.blockchain.balance.AssetCatalogue
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.coincore.bch.BchAsset
import piuk.blockchain.android.coincore.btc.BtcAsset
import piuk.blockchain.android.coincore.eth.EthAsset
import piuk.blockchain.android.coincore.fiat.FiatAsset
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.coincore.loader.AssetLoader
import piuk.blockchain.android.coincore.loader.CryptoAssetLoader
import piuk.blockchain.android.coincore.impl.BackendNotificationUpdater
import piuk.blockchain.android.coincore.impl.TxProcessorFactory
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.loader.AssetCatalogueImpl
import piuk.blockchain.android.coincore.loader.AssetRemoteFeatureLookup
import piuk.blockchain.android.coincore.xlm.XlmAsset
import piuk.blockchain.android.domain.repositories.AssetActivityRepository

val coincoreModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            BtcAsset(
                exchangeRates = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                currencyPrefs = get(),
                payloadManager = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                notificationUpdater = get(),
                coinsWebsocket = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            BchAsset(
                payloadManager = get(),
                bchDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                interestBalances = get(),
                tradingBalances = get(),
                feeDataManager = get(),
                sendDataManager = get(),
                pitLinking = get(),
                labels = get(),
                walletPreferences = get(),
                beNotifyUpdate = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            XlmAsset(
                payloadManager = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                walletOptionsDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            EthAsset(
                payloadManager = get(),
                ethDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                walletPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                pitLinking = get(),
                labels = get(),
                notificationUpdater = get(),
                identity = get(),
                features = get(),
                assetCatalogue = lazy { get() }
            )
        }.bind(CryptoAsset::class)

        scoped {
            FiatAsset(
                labels = get(),
                tradingBalanceDataManager = get(),
                exchangeRateDataManager = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            val fixedAssets: List<CryptoAsset> = payloadScope.getAll()
            Coincore(
                fixedAssets = fixedAssets,
                assetCatalogue = get(),
                payloadManager = get(),
                fiatAsset = get<FiatAsset>(),
                assetLoader = get(),
                txProcessorFactory = get(),
                defaultLabels = get(),
                crashLogger = get()
            )
        }

        scoped {
            CryptoAssetLoader(
                featureConfig = get(),
                payloadManager = get(),
                erc20DataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                tradingBalances = get(),
                interestBalances = get(),
                crashLogger = get(),
                labels = get(),
                pitLinking = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }.bind(AssetLoader::class)

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                lunuManager = get(),
                exchangeRates = get(),
                interestBalances = get(),
                walletManager = get(),
                walletPrefs = get(),
                quotesEngine = get(),
                analytics = get(),
                kycTierService = get()
            )
        }

        scoped {
            AssetActivityRepository(
                coincore = get()
            )
        }

        scoped {
            AddressFactoryImpl(
                coincore = get(),
                addressResolver = get(),
                features = get()
            )
        }.bind(AddressFactory::class)

        scoped {
            BackendNotificationUpdater(
                prefs = get(),
                walletApi = get()
            )
        }

        factory {
            TransferQuotesEngine(quotesProvider = get())
        }

        factory {
            LinkedBanksFactory(
                custodialWalletManager = get()
            )
        }

        factory {
            SwapTrendingPairsProvider(
                coincore = get(),
                identity = get()
            )
        }.bind(TrendingPairsProvider::class)
    }

    single {
        AssetRemoteFeatureLookup(
            remoteConfig = get()
        )
    }

    single {
        AssetCatalogueImpl(
            featureConfig = get()
        )
    }.bind(AssetCatalogue::class)
}