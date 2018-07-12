package piuk.blockchain.androidcore.data.settings.datastore

import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.datastores.persistentstore.DefaultFetchStrategy
import piuk.blockchain.androidcore.data.datastores.persistentstore.FreshFetchStrategy

class SettingsDataStore(
    private val memoryStore: SettingsMemoryStore,
    private val webSource: Observable<Settings>
) {

    fun getSettings(): Observable<Settings> =
        DefaultFetchStrategy(webSource, memoryStore.getSettings(), memoryStore).fetch()

    fun fetchSettings(): Observable<Settings> =
        FreshFetchStrategy(webSource, memoryStore).fetch()
}