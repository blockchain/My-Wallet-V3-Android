package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.extensions.wrapErrorMessage
import com.blockchain.kyc.models.nabu.AddAddressRequest
import com.blockchain.kyc.models.nabu.ApplicantIdRequest
import com.blockchain.kyc.models.nabu.NabuBasicUser
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuJwt
import com.blockchain.kyc.models.nabu.NabuStateResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.RecordCountryRequest
import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.SupportedDocuments
import com.blockchain.nabu.models.NabuOfflineTokenRequest
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.Retrofit

class NabuService(retrofit: Retrofit) {

    private val service: Nabu = retrofit.create(Nabu::class.java)

    internal fun getAuthToken(
        jwt: String
    ): Single<NabuOfflineTokenResponse> = service.getAuthToken(
        NabuOfflineTokenRequest(jwt)
    ).wrapErrorMessage()

    internal fun getSessionToken(
        userId: String,
        offlineToken: String,
        guid: String,
        email: String,
        appVersion: String,
        deviceId: String
    ): Single<NabuSessionTokenResponse> = service.getSessionToken(
        userId,
        offlineToken,
        guid,
        email,
        appVersion,
        CLIENT_TYPE,
        deviceId
    ).wrapErrorMessage()

    internal fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        sessionToken: NabuSessionTokenResponse
    ): Completable = service.createBasicUser(
        NabuBasicUser(firstName, lastName, dateOfBirth),
        sessionToken.authHeader
    )

    internal fun getUser(
        sessionToken: NabuSessionTokenResponse
    ): Single<NabuUser> = service.getUser(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun updateWalletInformation(
        sessionToken: NabuSessionTokenResponse,
        jwt: String
    ): Single<NabuUser> = service.updateWalletInformation(
        NabuJwt(jwt),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getCountriesList(
        scope: Scope
    ): Single<List<NabuCountryResponse>> = service.getCountriesList(
        scope.value
    ).wrapErrorMessage()

    internal fun getStatesList(
        countryCode: String,
        scope: Scope
    ): Single<List<NabuStateResponse>> = service.getStatesList(
        countryCode,
        scope.value
    ).wrapErrorMessage()

    internal fun getSupportedDocuments(
        sessionToken: NabuSessionTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>> = service.getSupportedDocuments(
        countryCode,
        sessionToken.authHeader
    ).wrapErrorMessage()
        .map { it.documentTypes }

    internal fun addAddress(
        sessionToken: NabuSessionTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable = service.addAddress(
        AddAddressRequest.fromAddressDetails(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recordCountrySelection(
        sessionToken: NabuSessionTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable = service.recordSelectedCountry(
        RecordCountryRequest(
            jwt,
            countryCode,
            notifyWhenAvailable,
            stateCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getOnfidoApiKey(
        sessionToken: NabuSessionTokenResponse
    ): Single<String> = service.getOnfidoApiKey(
        sessionToken.authHeader
    ).map { it.key }
        .wrapErrorMessage()

    internal fun getVeriffToken(
        sessionToken: NabuSessionTokenResponse
    ): Single<VeriffApplicantAndToken> = service.getVeriffToken(
        sessionToken.authHeader
    ).map { VeriffApplicantAndToken(it.applicantId, it.token) }
        .wrapErrorMessage()

    internal fun submitOnfidoVerification(
        sessionToken: NabuSessionTokenResponse,
        applicantId: String
    ): Completable = service.submitVerification(
        ApplicantIdRequest(applicantId),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun submitVeriffVerification(
        sessionToken: NabuSessionTokenResponse,
        applicantId: String
    ): Completable = service.submitVerification(
        ApplicantIdRequest(applicantId),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recoverUser(
        offlineToken: NabuOfflineTokenResponse,
        jwt: String
    ): Completable = service.recoverUser(
        offlineToken.userId,
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun registerCampaign(
        sessionToken: NabuSessionTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = service.registerCampaign(
        campaignRequest,
        campaignName,
        sessionToken.authHeader
    ).wrapErrorMessage()

    companion object {
        internal const val CLIENT_TYPE = "APP"
    }
}