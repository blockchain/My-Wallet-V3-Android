package com.blockchain.kyc.api.nabu

import com.blockchain.kyc.models.nabu.AddAddressRequest
import com.blockchain.kyc.models.nabu.AddMobileNumberRequest
import com.blockchain.kyc.models.nabu.Address
import com.blockchain.kyc.models.nabu.ApplicantIdRequest
import com.blockchain.kyc.models.nabu.MobileVerificationRequest
import com.blockchain.kyc.models.nabu.NabuBasicUser
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.NewUserRequest
import com.blockchain.kyc.models.nabu.OnfidoApiKey
import com.blockchain.kyc.models.nabu.UserId
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @PUT
    fun createBasicUser(
        @Url url: String,
        @Body basicUser: NabuBasicUser,
        @Header("authorization") authorization: String
    ): Completable

    @GET
    fun getUser(
        @Url url: String,
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET
    fun getCountriesList(
        @Url url: String,
        @Query("scope") scope: String?
    ): Single<List<NabuCountryResponse>>

    @GET
    fun findAddress(
        @Url url: String,
        @Query("postCode") postCode: String,
        @Query("countryCode") countryCode: String,
        @Header("authorization") authorization: String
    ): Single<List<Address>>

    @PUT
    fun addAddress(
        @Url url: String,
        @Body address: AddAddressRequest,
        @Header("authorization") authorization: String
    ): Completable

    @PUT
    fun addMobileNumber(
        @Url url: String,
        @Body mobileNumber: AddMobileNumberRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST
    fun verifyMobileNumber(
        @Url url: String,
        @Body mobileVerificationRequest: MobileVerificationRequest,
        @Header("authorization") authorization: String
    ): Completable

    @GET
    fun getOnfidoApiKey(
        @Url url: String,
        @Header("authorization") authorization: String
    ): Single<OnfidoApiKey>

    @POST
    fun submitOnfidoVerification(
        @Url url: String,
        @Body applicantIdRequest: ApplicantIdRequest,
        @Header("authorization") authorization: String
    ): Completable
}