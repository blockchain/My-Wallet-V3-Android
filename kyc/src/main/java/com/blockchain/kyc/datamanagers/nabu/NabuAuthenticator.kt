package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kycui.extensions.fetchNabuToken
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.NabuSessionTokenResponse
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager

internal class NabuAuthenticator(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager
) : Authenticator {

    override fun <T> authenticateSingle(singleFunction: (Single<NabuSessionTokenResponse>) -> Single<T>): Single<T> =
        metadataManager.fetchNabuToken()
            .map { nabuDataManager.currentToken(it) }
            .flatMap { singleFunction(it) }

    override fun <T> authenticate(singleFunction: (NabuSessionTokenResponse) -> Single<T>): Single<T> =
        metadataManager.fetchNabuToken()
            .flatMap { tokenResponse ->
                nabuDataManager.authenticate(tokenResponse, singleFunction)
                    .subscribeOn(Schedulers.io())
            }

    override fun invalidateToken() {
        nabuDataManager.invalidateToken()
    }
}
