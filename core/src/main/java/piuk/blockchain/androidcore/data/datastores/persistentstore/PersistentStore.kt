package piuk.blockchain.androidcore.data.datastores.persistentstore

import io.reactivex.Observable

interface PersistentStore<T> {

    fun store(data: T): Observable<T>

    fun invalidate()
}