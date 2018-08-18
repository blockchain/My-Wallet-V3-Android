package com.blockchain.koin

import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.contacts.Contacts
import info.blockchain.wallet.util.PrivateKeyFactory
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import piuk.blockchain.androidcore.data.contacts.ContactsService
import piuk.blockchain.androidcore.data.contacts.datastore.ContactsMapStore
import piuk.blockchain.androidcore.data.contacts.datastore.PendingTransactionListStore
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyFormatUtil
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payload.PayloadService
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.settings.SettingsService
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore
import piuk.blockchain.androidcore.data.settings.datastore.SettingsMemoryStore
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import piuk.blockchain.androidcore.utils.MetadataUtils
import piuk.blockchain.androidcore.utils.PrefsUtil

val coreModule = applicationContext {

    bean { RxBus() }

    bean { Contacts() }

    factory { ContactsService(get()) }

    bean { ContactsMapStore() }

    bean { PendingTransactionListStore() }

    factory { ContactsDataManager(get(), get(), get(), get()) }

    bean { EthDataStore() }

    factory { WalletOptionsDataManager(get(), get(), get(), get("explorer-url")) }

    factory { AuthService(get(), get()) }

    bean { WalletOptionsState() }

    bean { SettingsDataManager(get(), get(), get()) }

    bean { SettingsService(get()) }

    bean {
        SettingsDataStore(SettingsMemoryStore(), get<SettingsService>().getSettingsObservable())
    }

    factory { MetadataUtils() }

    factory { PrivateKeyFactory() }

    context("Payload") {

        factory { PayloadService(get()) }

        factory { PayloadDataManager(get(), get(), get(), get(), get()) }

        bean { MetadataManager(get(), get(), get()) }
    }

    bean { BchDataStore() }

    bean { BlockExplorer(get("explorer"), get("api"), getProperty("api-code")) }

    factory { ExchangeRateDataManager(get(), get()) }

    bean { ExchangeRateDataStore(get(), get()) }

    factory { ExchangeRateService(get()) }

    bean { PrefsUtil(get()) }

    factory { CurrencyFormatManager(get(), get(), get(), get(), get()) }

    factory { CurrencyFormatUtil() }

    bean { CurrencyState.getInstance() }
}
