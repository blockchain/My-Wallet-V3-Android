package com.blockchain.kyc.services.wallet

import com.blockchain.kyc.api.wallet.RETAIL_JWT_TOKEN
import com.blockchain.kyc.api.wallet.RetailWallet
import com.blockchain.kyc.models.wallet.RetailJwtResponse
import io.reactivex.Single
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Retrofit

class RetailWalletTokenService(
    environmentConfig: EnvironmentConfig,
    private val apiCode: String,
    retrofit: Retrofit
) {

    private val service: RetailWallet = retrofit.create(RetailWallet::class.java)
    private val explorerPath = environmentConfig.explorerUrl

    internal fun createUser(
        path: String = explorerPath + RETAIL_JWT_TOKEN,
        guid: String,
        sharedKey: String
    ): Single<RetailJwtResponse> = service.createUser(
        path,
        guid,
        sharedKey,
        apiCode
    )
}