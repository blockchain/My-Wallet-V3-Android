package com.blockchain.koin.modules

import android.content.Context
import com.blockchain.koin.eur
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.gbp
import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.moshiExplorerRetrofit
import com.blockchain.koin.mwaFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.usd
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.lifecycle.LifecycleInterestedComponent
import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentAccountMapper
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.blockchain.operations.AppStartUpFlushable
import com.blockchain.ui.password.SecondPasswordHandler
import com.blockchain.wallet.DefaultLabels
import com.google.gson.GsonBuilder
import info.blockchain.wallet.metadata.MetadataDerivation
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.cards.CardModel
import piuk.blockchain.android.cards.partners.EverypayCardActivator
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.BitPayService
import piuk.blockchain.android.data.biometrics.BiometricAuth
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.data.biometrics.CryptographyManager
import piuk.blockchain.android.data.biometrics.CryptographyManagerImpl
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.deeplink.BlockchainDeepLinkParser
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.deeplink.EmailVerificationDeepLinkHelper
import piuk.blockchain.android.deeplink.OpenBankingDeepLinkParser
import piuk.blockchain.android.identity.SiftDigitalTrust
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.simplebuy.EURPaymentAccountMapper
import piuk.blockchain.android.simplebuy.GBPPaymentAccountMapper
import piuk.blockchain.android.simplebuy.SimpleBuyFlowNavigator
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializer
import piuk.blockchain.android.simplebuy.SimpleBuyPrefsSerializerImpl
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.simplebuy.USDPaymentAccountMapper
import piuk.blockchain.android.sunriver.SunriverDeepLinkHelper
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.thepit.PitLinkingImpl
import piuk.blockchain.android.thepit.ThePitDeepLinkParser
import piuk.blockchain.android.ui.addresses.AccountPresenter
import piuk.blockchain.android.ui.airdrops.AirdropCentrePresenter
import piuk.blockchain.android.ui.auth.FirebaseMobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.MobileNoticeRemoteConfig
import piuk.blockchain.android.ui.auth.PinEntryPresenter
import piuk.blockchain.android.ui.recover.RecoverFundsPresenter
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingInteractor
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingModel
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingState
import piuk.blockchain.android.ui.backup.verify.BackupVerifyPresenter
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListPresenter
import piuk.blockchain.android.ui.createwallet.CreateWalletPresenter
import piuk.blockchain.android.ui.createwallet.NewCreateWalletPresenter
import piuk.blockchain.android.ui.customviews.SecondPasswordDialog
import piuk.blockchain.android.ui.customviews.dialogs.OverlayDetection
import piuk.blockchain.android.ui.dashboard.BalanceAnalyticsReporter
import piuk.blockchain.android.ui.dashboard.DashboardInteractor
import piuk.blockchain.android.ui.dashboard.DashboardModel
import piuk.blockchain.android.ui.dashboard.DashboardState
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsInteractor
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.home.CredentialsWiper
import piuk.blockchain.android.ui.home.MainPresenter
import piuk.blockchain.android.ui.kyc.email.entry.EmailVeriffModel
import piuk.blockchain.android.ui.kyc.email.entry.EmailVerifyInteractor
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.launcher.DeepLinkPersistence
import piuk.blockchain.android.ui.launcher.LauncherPresenter
import piuk.blockchain.android.ui.launcher.Prerequisites
import piuk.blockchain.android.ui.linkbank.BankAuthModel
import piuk.blockchain.android.ui.linkbank.BankAuthState
import piuk.blockchain.android.ui.onboarding.OnboardingPresenter
import piuk.blockchain.android.ui.pairingcode.PairingModel
import piuk.blockchain.android.ui.pairingcode.PairingState
import piuk.blockchain.android.ui.recover.AccountRecoveryInteractor
import piuk.blockchain.android.ui.recover.AccountRecoveryModel
import piuk.blockchain.android.ui.recover.AccountRecoveryState
import piuk.blockchain.android.data.GetAccumulatedInPeriodToIsFirstTimeBuyerMapper
import piuk.blockchain.android.data.GetNextPaymentDateListToFrequencyDateMapper
import piuk.blockchain.android.data.TradeDataManagerImpl
import piuk.blockchain.android.data.Mapper
import piuk.blockchain.android.data.api.bitpay.LunuDataManager
import piuk.blockchain.android.data.api.bitpay.LunuService
import piuk.blockchain.android.domain.repositories.TradeDataManager
import piuk.blockchain.android.domain.usecases.GetNextPaymentDateUseCase
import piuk.blockchain.android.domain.usecases.IsFirstTimeBuyerUseCase
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.resources.AssetResourcesImpl
import piuk.blockchain.android.ui.sell.BuySellFlowNavigator
import piuk.blockchain.android.ui.settings.SettingsPresenter
import piuk.blockchain.android.ui.shortcuts.receive.ReceiveQrPresenter
import piuk.blockchain.android.ui.ssl.SSLVerifyPresenter
import piuk.blockchain.android.ui.thepit.PitPermissionsPresenter
import piuk.blockchain.android.ui.thepit.PitVerifyEmailPresenter
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.android.ui.transfer.DashboardAccountsSorting
import piuk.blockchain.android.ui.transfer.receive.ReceiveIntentHelper
import piuk.blockchain.android.ui.transfer.receive.ReceiveModel
import piuk.blockchain.android.ui.transfer.receive.ReceiveState
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.CurrentContextAccess
import piuk.blockchain.android.util.FormatChecker
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.PrngHelper
import piuk.blockchain.android.util.ResourceDefaultLabels
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.ConnectionApi
import piuk.blockchain.androidcore.data.auth.metadata.WalletCredentialsMetadataUpdater
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.SSLVerifyUtil

val applicationModule = module {

    factory { OSUtil(get()) }

    factory { StringUtils(get()) }

    single {
        AppUtil(
            context = get(),
            payloadManager = get(),
            accessState = get(),
            prefs = get()
        )
    }

    factory { RootUtil() }

    single {
        CoinsWebSocketService(
            applicationContext = get()
        )
    }

    factory { get<Context>().resources }

    single { CurrentContextAccess() }

    single { LifecycleInterestedComponent() }
        .bind(LifecycleObservable::class)

    single {
        SiftDigitalTrust(
            accountId = BuildConfig.SIFT_ACCOUNT_ID,
            beaconKey = BuildConfig.SIFT_BEACON_KEY
        )
    }.bind(DigitalTrust::class)

    scope(payloadScopeQualifier) {

        factory {
            SecondPasswordDialog(contextAccess = get(), payloadManager = get())
        }.bind(SecondPasswordHandler::class)

        factory {
            KycStatusHelper(
                nabuDataManager = get(),
                nabuToken = get(),
                settingsDataManager = get(),
                tierService = get()
            )
        }

        scoped {
            CredentialsWiper(
                payloadManagerWiper = get(),
                accessState = get(),
                appUtil = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                metadataManager = get(),
                walletOptionsState = get(),
                nabuDataManager = get()
            )
        }

        factory {
            MainPresenter(
                prefs = get(),
                accessState = get(),
                credentialsWiper = get(),
                payloadDataManager = get(),
                qrProcessor = get(),
                kycStatusHelper = get(),
                deepLinkProcessor = get(),
                sunriverCampaignRegistration = get(),
                xlmDataManager = get(),
                pitLinking = get(),
                simpleBuySync = get(),
                crashLogger = get(),
                analytics = get(),
                bankLinkingPrefs = get(),
                custodialWalletManager = get(),
                upsellManager = get(),
                secureChannelManager = get(),
                payloadManager = get()
            )
        }

        scoped {
            SecureChannelManager(
                secureChannelPrefs = get(),
                authPrefs = get(),
                payloadManager = get(),
                walletApi = get()
            )
        }

        factory(gbp) {
            GBPPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        factory(eur) {
            EURPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        factory(usd) {
            USDPaymentAccountMapper(stringUtils = get())
        }.bind(PaymentAccountMapper::class)

        scoped {
            CoinsWebSocketStrategy(
                coinsWebSocket = get(),
                ethDataManager = get(),
                erc20DataManager = get(),
                bchDataManager = get(),
                stringUtils = get(),
                gson = get(),
                payloadDataManager = get(),
                rxBus = get(),
                prefs = get(),
                appUtil = get(),
                accessState = get(),
                assetCatalogue = get()
            )
        }

        factory {
            GsonBuilder().create()
        }

        factory {
            OkHttpClient()
                .newBlockchainWebSocket(options = Options(url = BuildConfig.COINS_WEBSOCKET_URL))
                .autoRetry()
                .debugLog("COIN_SOCKET")
        }

        factory {
            PairingModel(
                initialState = PairingState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                qrCodeDataManager = get(),
                payloadDataManager = get(),
                authDataManager = get()
            )
        }

        factory {
            CreateWalletPresenter(
                payloadDataManager = get(),
                prefs = get(),
                appUtil = get(),
                accessState = get(),
                prngFixer = get(),
                analytics = get(),
                walletPrefs = get(),
                environmentConfig = get(),
                formatChecker = get()
            )
        }

        factory {
            NewCreateWalletPresenter(
                payloadDataManager = get(),
                prefs = get(),
                appUtil = get(),
                accessState = get(),
                prngFixer = get(),
                analytics = get(),
                walletPrefs = get(),
                environmentConfig = get(),
                formatChecker = get(),
                nabuUserDataManager = get()
            )
        }

        factory {
            RecoverFundsPresenter(
                payloadDataManager = get(),
                prefs = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(),
                moshi = get(),
                analytics = get()
            )
        }

        factory {
            BackupWalletStartingInteractor(
                prefs = get(),
                settingsDataManager = get()
            )
        }

        factory {
            BackupWalletStartingModel(
                initialState = BackupWalletStartingState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            BackupWalletWordListPresenter(
                backupWalletUtil = get()
            )
        }

        factory {
            BackupWalletUtil(
                payloadDataManager = get()
            )
        }

        factory {
            BackupVerifyPresenter(
                payloadDataManager = get(),
                backupWalletUtil = get(),
                walletStatus = get()
            )
        }

        factory {
            AccountRecoveryModel(
                initialState = AccountRecoveryState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                interactor = get()
            )
        }

        factory {
            AccountRecoveryInteractor(
                payloadDataManager = get(),
                prefs = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(),
                nabuDataManager = get()
            )
        }

        factory {
            SunriverDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            KycDeepLinkHelper(
                linkHandler = get()
            )
        }

        factory {
            ThePitDeepLinkParser()
        }

        factory {
            BlockchainDeepLinkParser()
        }

        factory { EmailVerificationDeepLinkHelper() }

        factory {
            DeepLinkProcessor(
                linkHandler = get(),
                kycDeepLinkHelper = get(),
                sunriverDeepLinkHelper = get(),
                emailVerifiedLinkHelper = get(),
                thePitDeepLinkParser = get(),
                openBankingDeepLinkParser = get(),
                blockchainDeepLinkParser = get()
            )
        }

        factory {
            OpenBankingDeepLinkParser()
        }

        scoped {
            QrScanResultProcessor(
                bitPayDataManager = get(),
                lunuDataManager = get(),
                mwaFeatureFlag = get(mwaFeatureFlag)
            )
        }

        factory {
            AccountPresenter(
                privateKeyFactory = get(),
                analytics = get(),
                coincore = get()
            )
        }

        factory {
            ReceiveQrPresenter(
                payloadDataManager = get(),
                qrCodeDataManager = get()
            )
        }

        factory {
            DeepLinkPersistence(
                prefs = get()
            )
        }

        factory {
            DashboardModel(
                initialState = DashboardState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            DashboardInteractor(
                coincore = get(),
                payloadManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                custodialWalletManager = get(),
                simpleBuyPrefs = get(),
                analytics = get(),
                crashLogger = get(),
                linkedBanksFactory = get()
            )
        }

        scoped {
            AssetDetailsModel(
                initialState = AssetDetailsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            AssetDetailsInteractor(
                interestFeatureFlag = get(interestAccountFeatureFlag),
                dashboardPrefs = get(),
                coincore = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            SimpleBuyInteractor(
                withdrawLocksRepository = get(),
                tierService = get(),
                custodialWalletManager = get(),
                appUtil = get(),
                coincore = get(),
                eligibilityProvider = get(),
                bankLinkingPrefs = get(),
                analytics = get()
            )
        }

        factory {
            SimpleBuyModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                initialState = SimpleBuyState(),
                ratingPrefs = get(),
                prefs = get(),
                serializer = get(),
                cardActivators = listOf(
                    EverypayCardActivator(get(), get())
                ),
                environmentConfig = get(),
                crashLogger = get(),
                isFirstTimeBuyerUseCase = get(),
                getNextPaymentDateUseCase = get(),
                featureFlagApi = get()
            )
        }

        factory {
            IsFirstTimeBuyerUseCase(
                tradeDataManager = get()
            )
        }

        factory {
            GetNextPaymentDateUseCase(
                tradeDataManager = get()
            )
        }

        factory {
            TradeDataManagerImpl(
                tradeService = get(),
                authenticator = get(),
                accumulatedInPeriodMapper = get(),
                nextPaymentDateMapper = get()
            )
        }.bind(TradeDataManager::class)

        factory {
            GetAccumulatedInPeriodToIsFirstTimeBuyerMapper()
        }.bind(Mapper::class)

        factory {
            GetNextPaymentDateListToFrequencyDateMapper()
        }.bind(Mapper::class)

        factory {
            SimpleBuyPrefsSerializerImpl(
                prefs = get(),
                assetCatalogue = get()
            )
        }.bind(SimpleBuyPrefsSerializer::class)

        factory {
            BankAuthModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                initialState = BankAuthState(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            CardModel(
                interactor = get(),
                currencyPrefs = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                cardActivators = listOf(
                    EverypayCardActivator(get(), get())
                ),
                gson = get(),
                prefs = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            SimpleBuyFlowNavigator(
                simpleBuyModel = get(),
                tierService = get(),
                currencyPrefs = get(),
                custodialWalletManager = get()
            )
        }

        factory {
            BuySellFlowNavigator(
                simpleBuyModel = get(),
                custodialWalletManager = get(),
                currencyPrefs = get(),
                tierService = get(),
                eligibilityProvider = get()
            )
        }

        scoped {
            SimpleBuySyncFactory(
                custodialWallet = get(),
                serializer = get()
            )
        }

        factory {
            BalanceAnalyticsReporter(
                analytics = get(),
                coincore = get()
            )
        }

        factory {
            ReceiveModel(
                initialState = ReceiveState(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                qrCodeDataManager = get(),
                receiveIntentHelper = get()
            )
        }

        factory {
            KycUpgradePromptManager(
                identity = get()
            )
        }

        factory {
            SettingsPresenter(
                authDataManager = get(),
                settingsDataManager = get(),
                emailUpdater = get(),
                payloadManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                accessState = get(),
                custodialWalletManager = get(),
                notificationTokenManager = get(),
                exchangeRates = get(),
                kycStatusHelper = get(),
                pitLinking = get(),
                analytics = get(),
                biometricsController = get(),
                ratingPrefs = get(),
                qrProcessor = get(),
                secureChannelManager = get()
            )
        }

        factory {
            PinEntryPresenter(
                authDataManager = get(),
                appUtil = get(),
                prefs = get(),
                payloadDataManager = get(),
                defaultLabels = get(),
                accessState = get(),
                walletOptionsDataManager = get(),
                prngFixer = get(),
                mobileNoticeRemoteConfig = get(),
                crashLogger = get(),
                analytics = get(),
                apiStatus = get(),
                credentialsWiper = get(),
                biometricsController = get()
            )
        }

        scoped {
            PitLinkingImpl(
                nabu = get(),
                nabuToken = get(),
                payloadDataManager = get(),
                ethDataManager = get(),
                bchDataManager = get(),
                xlmDataManager = get()
            )
        }.bind(PitLinking::class)

        factory {
            BitPayDataManager(
                bitPayService = get()
            )
        }

        factory {
            LunuDataManager(
                lunuService = get()
            )
        }

        factory {
            BitPayService(
                environmentConfig = get(),
                retrofit = get(moshiExplorerRetrofit),
            )
        }

        factory {
            LunuService(
                environmentConfig = get(),
                retrofit = get(moshiExplorerRetrofit),
            )
        }

        factory {
            PitPermissionsPresenter(
                nabu = get(),
                nabuToken = get(),
                pitLinking = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            PitVerifyEmailPresenter(
                nabuToken = get(),
                nabu = get(),
                emailSyncUpdater = get()
            )
        }

        factory {
            BackupWalletCompletedPresenter(
                walletStatus = get(),
                authDataManager = get()
            )
        }

        factory {
            OnboardingPresenter(
                biometricsController = get(),
                accessState = get(),
                settingsDataManager = get()
            )
        }

        factory {
            LauncherPresenter(
                appUtil = get(),
                payloadDataManager = get(),
                prefs = get(),
                deepLinkPersistence = get(),
                accessState = get(),
                settingsDataManager = get(),
                notificationTokenManager = get(),
                envSettings = get(),
                currencyPrefs = get(),
                analytics = get(),
                crashLogger = get(),
                prerequisites = get(),
                userIdentity = get(),
                walletPrefs = get(),
                nabuUserDataManager = get()
            )
        }

        factory {
            Prerequisites(
                metadataManager = get(),
                settingsDataManager = get(),
                coincore = get(),
                exchangeRates = get(),
                crashLogger = get(),
                simpleBuySync = get(),
                rxBus = get(),
                flushables = getAll(AppStartUpFlushable::class),
                walletCredentialsUpdater = get()
            )
        }

        factory {
            WalletCredentialsMetadataUpdater(
                payloadDataManager = get(),
                metadataRepository = get()
            )
        }

        factory {
            AirdropCentrePresenter(
                nabuToken = get(),
                nabu = get(),
                assetCatalogue = get(),
                crashLogger = get()
            )
        }

        factory {
            BiometricsController(
                applicationContext = get(),
                prefs = get(),
                accessState = get(),
                cryptographyManager = get(),
                crashLogger = get()
            )
        }.bind(BiometricAuth::class)

        factory {
            CryptographyManagerImpl()
        }.bind(CryptographyManager::class)

        factory {
            EmailVeriffModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            EmailVerifyInteractor(emailUpdater = get())
        }

        factory {
            DashboardAccountsSorting(
                dashboardPrefs = get(),
                assetCatalogue = get()
            )
        }.bind(AccountsSorting::class)
    }

    factory {
        FirebaseMobileNoticeRemoteConfig(remoteConfig = get())
    }.bind(MobileNoticeRemoteConfig::class)

    factory {
        QrCodeDataManager()
    }

    single {
        PrngHelper(
            context = get(),
            accessState = get()
        )
    }.bind(PrngFixer::class)

    single {
        ConnectionApi(retrofit = get(explorerRetrofit))
    }

    single {
        SSLVerifyUtil(connectionApi = get())
    }

    factory {
        SSLVerifyPresenter(
            sslVerifyUtil = get()
        )
    }

    factory {
        ResourceDefaultLabels(
            resources = get(),
            assetResources = get()
        )
    }.bind(DefaultLabels::class)

    single {
        OverlayDetection(get())
    }

    single {
        AssetResourcesImpl(
            resources = get()
        )
    }.bind(AssetResources::class)

    factory {
        ReceiveIntentHelper(
            context = get()
        )
    }

    factory { FormatChecker() }
}
