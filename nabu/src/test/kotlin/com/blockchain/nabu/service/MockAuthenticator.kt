package com.blockchain.nabu.service

import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.NabuSessionTokenResponse
import io.reactivex.Single

class MockAuthenticator(val token: String) : Authenticator {

    override fun <T> authenticate(singleFunction: (NabuSessionTokenResponse) -> Single<T>): Single<T> {
        return Single.just(
            NabuSessionTokenResponse(
                id = "",
                userId = "",
                token = token,
                isActive = true,
                expiresAt = "",
                insertedAt = "",
                updatedAt = ""
            )
        ).flatMap { singleFunction(it) }
    }
}
