package piuk.blockchain.androidcore.data.datastores.persistentstore

import com.blockchain.data.datastores.PersistentStore
import io.reactivex.Observable

/**
 * Fetches data from the web and then stores it in memory
 */
class FreshFetchStrategy<T>(
    private val webSource: Observable<T>,
    private val memoryStore: PersistentStore<T>
) : FetchStrategy<T>() {

    override fun fetch(): Observable<T> = webSource.flatMap(memoryStore::store)
}