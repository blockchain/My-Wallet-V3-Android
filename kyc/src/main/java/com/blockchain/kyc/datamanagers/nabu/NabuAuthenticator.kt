package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kycui.extensions.fetchNabuToken
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.NabuSessionTokenResponse
import io.reactivex.Single
import piuk.blockchain.androidcore.data.metadata.MetadataManager

internal class NabuAuthenticator(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager
) : Authenticator {
    override fun <T> authenticate(singleFunction: (NabuSessionTokenResponse) -> Single<T>): Single<T> =
        metadataManager.fetchNabuToken()
            .flatMap { tokenResponse ->
                nabuDataManager.authenticate(tokenResponse, singleFunction)
            }
}
