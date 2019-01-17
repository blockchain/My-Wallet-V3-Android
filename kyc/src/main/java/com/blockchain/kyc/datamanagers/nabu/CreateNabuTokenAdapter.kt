package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import io.reactivex.Single

internal class CreateNabuTokenAdapter(
    private val nabuDataManager: NabuDataManager
) : CreateNabuToken {

    override fun createNabuOfflineToken(): Single<NabuOfflineTokenResponse> =
        nabuDataManager.requestJwt()
            .flatMap { jwt ->
                nabuDataManager.getAuthToken(jwt)
            }
}
