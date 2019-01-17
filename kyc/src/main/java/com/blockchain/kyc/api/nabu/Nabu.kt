package com.blockchain.kyc.api.nabu

import com.blockchain.kyc.models.nabu.AddAddressRequest
import com.blockchain.kyc.models.nabu.ApplicantIdRequest
import com.blockchain.kyc.models.nabu.NabuBasicUser
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuJwt
import com.blockchain.kyc.models.nabu.NabuStateResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.OnfidoApiKey
import com.blockchain.kyc.models.nabu.RecordCountryRequest
import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
import com.blockchain.kyc.models.nabu.SupportedDocumentsResponse
import com.blockchain.kyc.models.nabu.TierUpdateJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.models.nabu.VeriffToken
import com.blockchain.nabu.models.NabuOfflineTokenRequest
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.NabuSessionTokenResponse
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface Nabu {

    @POST(NABU_INITIAL_AUTH)
    fun getAuthToken(
        @Body jwt: NabuOfflineTokenRequest
    ): Single<NabuOfflineTokenResponse>

    @POST(NABU_SESSION_TOKEN)
    fun getSessionToken(
        @Query("userId") userId: String,
        @Header("authorization") authorization: String,
        @Header("X-WALLET-GUID") guid: String,
        @Header("X-WALLET-EMAIL") email: String,
        @Header("X-APP-VERSION") appVersion: String,
        @Header("X-CLIENT-TYPE") clientType: String,
        @Header("X-DEVICE-ID") deviceId: String
    ): Single<NabuSessionTokenResponse>

    @PUT(NABU_USERS_CURRENT)
    fun createBasicUser(
        @Body basicUser: NabuBasicUser,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_USERS_CURRENT)
    fun getUser(
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @PUT(NABU_UPDATE_WALLET_INFO)
    fun updateWalletInformation(
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET(NABU_COUNTRIES)
    fun getCountriesList(
        @Query("scope") scope: String?
    ): Single<List<NabuCountryResponse>>

    @GET("$NABU_COUNTRIES/{regionCode}/$NABU_STATES")
    fun getStatesList(
        @Path("regionCode") countryCode: String,
        @Query("scope") scope: String?
    ): Single<List<NabuStateResponse>>

    @GET("$NABU_SUPPORTED_DOCUMENTS/{countryCode}")
    fun getSupportedDocuments(
        @Path("countryCode") countryCode: String,
        @Header("authorization") authorization: String
    ): Single<SupportedDocumentsResponse>

    @PUT(NABU_PUT_ADDRESS)
    fun addAddress(
        @Body address: AddAddressRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST(NABU_RECORD_COUNTRY)
    fun recordSelectedCountry(
        @Body recordCountryRequest: RecordCountryRequest,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_ONFIDO_API_KEY)
    fun getOnfidoApiKey(
        @Header("authorization") authorization: String
    ): Single<OnfidoApiKey>

    @GET(NABU_VERIFF_TOKEN)
    fun getVeriffToken(
        @Header("authorization") authorization: String
    ): Single<VeriffToken>

    @POST(NABU_SUBMIT_VERIFICATION)
    fun submitVerification(
        @Body applicantIdRequest: ApplicantIdRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST("$NABU_RECOVER_USER/{userId}")
    fun recoverUser(
        @Path("userId") userId: String,
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_REGISTER_CAMPAIGN)
    fun registerCampaign(
        @Body campaignRequest: RegisterCampaignRequest,
        @Header("X-CAMPAIGN") campaignHeader: String,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_KYC_TIERS)
    fun getTiers(
        @Header("authorization") authorization: String
    ): Single<TiersJson>

    @POST(NABU_KYC_TIERS)
    fun setTier(
        @Body tierUpdateJson: TierUpdateJson,
        @Header("authorization") authorization: String
    ): Completable
}