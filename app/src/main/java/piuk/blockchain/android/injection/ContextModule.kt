package piuk.blockchain.android.injection

import android.content.Context
import com.blockchain.koin.KoinDaggerModule
import dagger.Module
import dagger.Provides
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.contacts.datastore.ContactsMapStore
import piuk.blockchain.androidcore.data.contacts.datastore.PendingTransactionListStore
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import piuk.blockchain.androidcore.utils.PrefsUtil
import javax.inject.Singleton

@Module
class ContextModule(private val appContext: Context) : KoinDaggerModule() {

    @Singleton
    @Provides
    fun appContext(): Context = appContext

    @Provides
    fun provideRxBus(): RxBus {
        return get(RxBus::class)
    }

    @Provides
    fun provideEthDataStore(): EthDataStore {
        return get(EthDataStore::class)
    }

    @Provides
    fun provideBchDataStore(): BchDataStore {
        return get(BchDataStore::class)
    }

    @Provides
    fun provideContactsMapStore(): ContactsMapStore {
        return get(ContactsMapStore::class)
    }

    @Provides
    fun providePendingTransactionListStore(): PendingTransactionListStore {
        return get(PendingTransactionListStore::class)
    }

    @Provides
    fun provideWalletOptionsState(): WalletOptionsState {
        return get(WalletOptionsState::class)
    }

    @Provides
    fun provideExchangeRateDataStore(): ExchangeRateDataStore {
        return get(ExchangeRateDataStore::class)
    }

    @Provides
    fun providePrefsUtil(): PrefsUtil {
        return get(PrefsUtil::class)
    }
}