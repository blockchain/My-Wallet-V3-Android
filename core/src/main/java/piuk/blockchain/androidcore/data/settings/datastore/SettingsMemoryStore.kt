package piuk.blockchain.androidcore.data.settings.datastore

import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import com.blockchain.data.datastores.PersistentStore
import com.blockchain.utils.Optional

class SettingsMemoryStore : SettingsStore, PersistentStore<Settings> {

    private var settings: Optional<Settings> = Optional.None

    override fun store(data: Settings): Observable<Settings> {
        settings = Optional.Some(data)
        return Observable.just((settings as Optional.Some<Settings>).element)
    }

    override fun getSettings(): Observable<Optional<Settings>> = Observable.just(settings)

    override fun invalidate() {
        settings = Optional.None
    }
}