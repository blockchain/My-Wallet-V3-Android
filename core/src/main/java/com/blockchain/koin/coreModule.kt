package com.blockchain.koin

import android.content.Context
import com.blockchain.accounts.AccountList
import com.blockchain.accounts.AllAccountList
import com.blockchain.accounts.AllAccountsImplementation
import com.blockchain.accounts.AsyncAccountList
import com.blockchain.accounts.AsyncAllAccountList
import com.blockchain.accounts.AsyncAllAccountListImplementation
import com.blockchain.accounts.BchAccountListAdapter
import com.blockchain.accounts.BchAsyncAccountListAdapter
import com.blockchain.accounts.BtcAccountListAdapter
import com.blockchain.accounts.BtcAsyncAccountListAdapter
import com.blockchain.accounts.EthAccountListAdapter
import com.blockchain.accounts.EthAsyncAccountListAdapter
import com.blockchain.balance.AsyncAccountBalanceReporter
import com.blockchain.balance.AsyncAddressBalanceReporter
import com.blockchain.balance.BchBalanceAdapter
import com.blockchain.balance.BtcBalanceAdapter
import com.blockchain.balance.EthBalanceAdapter
import com.blockchain.balance.plus
import com.blockchain.datamanagers.AccountLookup
import com.blockchain.datamanagers.AddressResolver
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.datamanagers.MaximumSpendableCalculatorImplementation
import com.blockchain.datamanagers.TransactionSendDataManager
import com.blockchain.logging.LastTxUpdateDateOnSettingsService
import com.blockchain.logging.LastTxUpdater
import com.blockchain.logging.NullLogger
import com.blockchain.logging.TimberLogger
import com.blockchain.metadata.MetadataRepository
import com.blockchain.preferences.FiatCurrencyPreference
import com.blockchain.wallet.DefaultLabels
import com.blockchain.wallet.ResourceDefaultLabels
import com.blockchain.wallet.SeedAccess
import com.blockchain.wallet.SeedAccessWithoutPrompt
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.contacts.Contacts
import info.blockchain.wallet.util.PrivateKeyFactory
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.BuildConfig
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import piuk.blockchain.androidcore.data.contacts.ContactsService
import piuk.blockchain.androidcore.data.contacts.datastore.ContactsMapStore
import piuk.blockchain.androidcore.data.contacts.datastore.PendingTransactionListStore
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyFormatUtil
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.exchangerate.ratesFor
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.metadata.MoshiMetadataRepositoryAdapter
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManagerSeedAccessAdapter
import piuk.blockchain.androidcore.data.payload.PayloadService
import piuk.blockchain.androidcore.data.payload.PromptingSeedAccessAdapter
import piuk.blockchain.androidcore.data.payments.PaymentService
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.PhoneVerificationQuery
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.settings.SettingsEmailAndSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsPhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.SettingsPhoneVerificationQuery
import piuk.blockchain.androidcore.data.settings.SettingsService
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore
import piuk.blockchain.androidcore.data.settings.datastore.SettingsMemoryStore
import piuk.blockchain.androidcore.data.transactions.TransactionListStore
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import piuk.blockchain.androidcore.utils.AESUtilWrapper
import piuk.blockchain.androidcore.utils.MetadataUtils
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.SharedPreferencesFiatCurrencyPreference

val coreModule = applicationContext {

    bean { RxBus() }

    factory { AuthService(get(), get()) }

    factory { MetadataUtils() }

    factory { PrivateKeyFactory() }

    context("Payload") {

        factory { PayloadService(get()) }

        factory { PayloadDataManager(get(), get(), get(), get(), get()) }

        factory { PromptingSeedAccessAdapter(PayloadDataManagerSeedAccessAdapter(get()), get()) }
            .bind(SeedAccessWithoutPrompt::class)
            .bind(SeedAccess::class)

        bean { MetadataManager(get(), get(), get()) }

        bean { MoshiMetadataRepositoryAdapter(get(), get()) as MetadataRepository }

        factory { AddressResolver(get(), get(), get()) }

        factory { AccountLookup(get(), get(), get()) }

        factory { TransactionSendDataManager(get(), get(), get(), get(), get(), get(), get(), get()) }

        factory { MaximumSpendableCalculatorImplementation(get(), get()) as MaximumSpendableCalculator }

        factory("BTC") { BtcAccountListAdapter(get()) as AccountList }
        factory("BCH") { BchAccountListAdapter(get()) as AccountList }
        factory("ETH") { EthAccountListAdapter(get()) as AccountList }

        factory("BTC") { BtcAsyncAccountListAdapter(get()) as AsyncAccountList }
        factory("BCH") { BchAsyncAccountListAdapter(get()) as AsyncAccountList }
        factory("ETH") { EthAsyncAccountListAdapter(EthAccountListAdapter(get())) as AsyncAccountList }

        factory("BTC") { BtcBalanceAdapter(get()) }
            .bind(AsyncAddressBalanceReporter::class)
            .bind(AsyncAccountBalanceReporter::class)
        factory("BCH") { BchBalanceAdapter(get()) }
            .bind(AsyncAddressBalanceReporter::class)
            .bind(AsyncAccountBalanceReporter::class)
        factory("ETH") { EthBalanceAdapter(get()) }
            .bind(AsyncAddressBalanceReporter::class)
            .bind(AsyncAccountBalanceReporter::class)

        factory("all") {
            get<AsyncAccountBalanceReporter>("BTC") +
                get("BCH") + get("ETH") + get("XLM")
        }

        factory {
            AllAccountsImplementation(
                btcAccountList = get("BTC"),
                bchAccountList = get("BCH"),
                etherAccountList = get("ETH")
            ) as AllAccountList
        }

        factory {
            AsyncAllAccountListImplementation(
                listOf(
                    get("BTC"),
                    get("ETH"),
                    get("BCH"),
                    get("XLM")
                )
            ) as AsyncAllAccountList
        }

        bean { EthDataStore() }

        bean { BchDataStore() }

        bean { ContactsMapStore() }

        bean { PendingTransactionListStore() }

        bean { WalletOptionsState() }

        bean { SettingsDataManager(get(), get(), get()) }

        bean { SettingsService(get()) }

        bean {
            SettingsDataStore(SettingsMemoryStore(), get<SettingsService>().getSettingsObservable())
        }

        bean { Contacts() }

        factory { ContactsService(get()) }

        factory { ContactsDataManager(get(), get(), get(), get()) }

        factory { WalletOptionsDataManager(get(), get(), get(), get("explorer-url")) }

        factory { ExchangeRateDataManager(get(), get()) }

        bean { ExchangeRateDataStore(get(), get()) }

        /**
         * Yields a FiatExchangeRates preset for the users preferred currency and suitable for use in CryptoValue.toFiat
         */
        factory { get<ExchangeRateDataManager>().ratesFor(get<FiatCurrencyPreference>()) }

        factory { FeeDataManager(get(), get(), get(), get()) }

        bean { TransactionListStore() }

        factory { CurrencyFormatManager(get(), get(), get(), get(), get()) }

        factory { AuthDataManager(get(), get(), get(), get(), get()) }

        factory { LastTxUpdateDateOnSettingsService(get()) as LastTxUpdater }

        factory { SendDataManager(get(), get(), get()) }

        factory { SettingsPhoneVerificationQuery(get()) as PhoneVerificationQuery }

        factory { SettingsPhoneNumberUpdater(get()) as PhoneNumberUpdater }

        factory { SettingsEmailAndSyncUpdater(get(), get()) as EmailSyncUpdater }
    }

    bean { BlockExplorer(get("explorer"), get("api"), getProperty("api-code")) }

    factory { ExchangeRateService(get()) }

    bean { PrefsUtil(get()) }

    bean { SharedPreferencesFiatCurrencyPreference(get()) as FiatCurrencyPreference }

    factory { CurrencyFormatUtil() }

    bean { CurrencyState(get()) }

    factory { PaymentService(get(), get(), get()) }

    bean {
        if (BuildConfig.DEBUG)
            TimberLogger()
        else
            NullLogger
    }

    factory { EthereumAccountWrapper() }

    factory { AccessState.getInstance() }

    factory {
        val accessState = get<AccessState>()
        object : LogoutTimer {
            override fun start(context: Context) {
                accessState.startLogoutTimer(context)
            }

            override fun stop(context: Context) {
                accessState.stopLogoutTimer(context)
            }
        } as LogoutTimer
    }

    factory { AESUtilWrapper() }

    factory { ResourceDefaultLabels(get()) as DefaultLabels }
}
