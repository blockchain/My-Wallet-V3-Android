package com.blockchain.kyc.api.wallet

import com.blockchain.kyc.models.wallet.RetailJwtResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface RetailWallet {

    @GET
    fun createUser(
        @Url url: String,
        @Query("guid") guid: String,
        @Query("sharedKey") sharedKey: String,
        @Query("api_code") apiCode: String
    ): Single<RetailJwtResponse>
}