package com.blockchain.data.datastores

import io.reactivex.Observable

interface PersistentStore<T> {

    fun store(data: T): Observable<T>

    fun invalidate()
}