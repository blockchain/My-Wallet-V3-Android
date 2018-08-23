package com.blockchain.nabu.stores

import com.blockchain.data.datastores.PersistentStore
import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.utils.Optional
import io.reactivex.Observable

class NabuSessionTokenStore : NabuTokenStore, PersistentStore<NabuSessionTokenResponse> {

    private var token: Optional<NabuSessionTokenResponse> = Optional.None

    override fun store(data: NabuSessionTokenResponse): Observable<NabuSessionTokenResponse> {
        token = Optional.Some(data)
        return Observable.just(getElement())
    }

    override fun getAccessToken(): Observable<Optional<NabuSessionTokenResponse>> =
        Observable.just(token)

    override fun invalidate() {
        token = Optional.None
    }

    fun requiresRefresh(): Boolean = when (token) {
        is Optional.None -> true
        else -> false
    }

    private fun getElement(): NabuSessionTokenResponse = (token as Optional.Some).element
}