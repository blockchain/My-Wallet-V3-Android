package piuk.blockchain.androidcore.data.datastores.persistentstore

import io.reactivex.Observable

abstract class FetchStrategy<T> {

    abstract fun fetch(): Observable<T>
}