package com.blockchain.kyc.api.nabu

import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.models.nabu.NewUserRequest
import com.blockchain.kyc.models.nabu.UserId
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

internal interface Nabu {

    @POST
    fun createUser(
        @Url url: String,
        @Body userRequest: NewUserRequest,
        @Header("authorization") authorization: String
    ): Single<UserId>

    @POST
    fun getAuthToken(
        @Url url: String,
        @Query("userId") userId: String,
        @Header("authorization") authorization: String,
        @Header("X-WALLET-GUID") guid: String,
        @Header("X-WALLET-EMAIL") email: String,
        @Header("X-APP-VERSION") appVersion: String,
        @Header("X-CLIENT-TYPE") clientType: String,
        @Header("X-DEVICE-ID") deviceId: String
    ): Single<NabuOfflineTokenResponse>

    @POST
    fun getSessionToken(
        @Url url: String,
        @Query("userId") userId: String,
        @Header("authorization") authorization: String,
        @Header("X-WALLET-GUID") guid: String,
        @Header("X-WALLET-EMAIL") email: String,
        @Header("X-APP-VERSION") appVersion: String,
        @Header("X-CLIENT-TYPE") clientType: String,
        @Header("X-DEVICE-ID") deviceId: String
    ): Single<NabuSessionTokenResponse>

    @GET
    fun getHomebrewRegions(
        @Url url: String,
        @Query("region") region: String
    ): Single<List<NabuCountryResponse>>
}