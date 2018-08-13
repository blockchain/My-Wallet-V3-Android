package com.blockchain.kyc.stores

import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.datastores.persistentstore.PersistentStore
import piuk.blockchain.androidcore.utils.Optional

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

    internal fun requiresRefresh(): Boolean = when (token) {
        is Optional.None -> true
        else -> false
    }

    private fun getElement(): NabuSessionTokenResponse = (token as Optional.Some).element
}