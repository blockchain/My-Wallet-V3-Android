package com.blockchain.nabu

import com.blockchain.nabu.models.NabuSessionTokenResponse
import io.reactivex.Single

interface Authenticator {

    fun <T> authenticate(
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T>

    fun <T> authenticateSingle(
        singleFunction: (Single<NabuSessionTokenResponse>) -> Single<T>
    ): Single<T>

    fun authenticate(): Single<NabuSessionTokenResponse> =
        authenticateSingle { it }

    fun invalidateToken()
}
