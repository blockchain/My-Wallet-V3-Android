package com.blockchain.kyc.stores

import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.datastores.persistentstore.PersistentStore
import piuk.blockchain.androidcore.utils.Optional

class NabuSessionTokenStore : NabuTokenStore, PersistentStore<NabuOfflineTokenResponse> {

    private var token: Optional<NabuOfflineTokenResponse> = Optional.None

    override fun store(data: NabuOfflineTokenResponse): Observable<NabuOfflineTokenResponse> {
        token = Optional.Some(data)
        return Observable.just(getElement())
    }

    override fun getAccessToken(): Observable<Optional<NabuOfflineTokenResponse>> =
        Observable.just(token)

    override fun invalidate() {
        token = Optional.None
    }

    internal fun requiresRefresh(): Boolean = when (token) {
        is Optional.None -> true
        else -> false
    }

    private fun getElement(): NabuOfflineTokenResponse = (token as Optional.Some).element
}