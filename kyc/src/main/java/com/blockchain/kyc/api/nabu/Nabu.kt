package com.blockchain.kyc.api.nabu

import com.blockchain.kyc.models.nabu.NabuCountryResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface Nabu {

    @GET
    fun getHomebrewRegions(
        @Url url: String,
        @Query("region") region: String,
        @Header("User-Agent") userAgent: String
    ): Single<List<NabuCountryResponse>>
}