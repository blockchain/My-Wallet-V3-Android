package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.NABU_COUNTRIES
import com.blockchain.kyc.api.nabu.NABU_CREATE_USER_ID
import com.blockchain.kyc.api.nabu.NABU_INITIAL_AUTH
import com.blockchain.kyc.api.nabu.NABU_PUT_ADDRESS
import com.blockchain.kyc.api.nabu.NABU_PUT_MOBILE
import com.blockchain.kyc.api.nabu.NABU_SESSION_TOKEN
import com.blockchain.kyc.api.nabu.NABU_USERS_CURRENT
import com.blockchain.kyc.api.nabu.NABU_VERIFICAITIONS
import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.extensions.wrapErrorMessage
import com.blockchain.kyc.models.nabu.AddAddressRequest
import com.blockchain.kyc.models.nabu.AddMobileNumberRequest
import com.blockchain.kyc.models.nabu.MobileVerificationRequest
import com.blockchain.kyc.models.nabu.NabuBasicUser
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.NewUserRequest
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.UserId
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Retrofit

class NabuService(
    val environmentConfig: EnvironmentConfig,
    retrofit: Retrofit
) {

    private val service: Nabu = retrofit.create(Nabu::class.java)
    private val apiPath = environmentConfig.apiUrl

    internal fun createUserId(
        path: String = apiPath + NABU_CREATE_USER_ID,
        guid: String,
        email: String
    ): Single<UserId> = service.createUser(
        path,
        NewUserRequest(email, guid),
        ""
    ).wrapErrorMessage()

    internal fun getAuthToken(
        path: String = apiPath + NABU_INITIAL_AUTH,
        guid: String,
        email: String,
        userId: String,
        appVersion: String,
        deviceId: String
    ): Single<NabuOfflineTokenResponse> = service.getAuthToken(
        path,
        userId,
        "",
        guid,
        email,
        appVersion,
        CLIENT_TYPE,
        deviceId
    ).wrapErrorMessage()

    internal fun getSessionToken(
        path: String = apiPath + NABU_SESSION_TOKEN,
        userId: String,
        offlineToken: String,
        guid: String,
        email: String,
        appVersion: String,
        deviceId: String
    ): Single<NabuSessionTokenResponse> = service.getSessionToken(
        path,
        userId,
        offlineToken,
        guid,
        email,
        appVersion,
        CLIENT_TYPE,
        deviceId
    ).wrapErrorMessage()

    internal fun createBasicUser(
        path: String = apiPath + NABU_USERS_CURRENT,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        sessionToken: String
    ): Completable = service.createBasicUser(
        path,
        NabuBasicUser(firstName, lastName, dateOfBirth),
        sessionToken
    )

    internal fun getUser(
        path: String = apiPath + NABU_USERS_CURRENT,
        sessionToken: String
    ): Single<NabuUser> = service.getUser(
        path,
        sessionToken
    ).wrapErrorMessage()

    internal fun getCountriesList(
        path: String = apiPath + NABU_COUNTRIES,
        scope: Scope
    ): Single<List<NabuCountryResponse>> = service.getCountriesList(
        path,
        scope.value
    ).wrapErrorMessage()

    internal fun addAddress(
        path: String = apiPath + NABU_PUT_ADDRESS,
        city: String,
        line1: String,
        line2: String?,
        state: String?,
        countryCode: String,
        postCode: String,
        sessionToken: String
    ): Completable = service.addAddress(
        path,
        AddAddressRequest.fromAddressDetails(
            city,
            line1,
            line2,
            state,
            countryCode,
            postCode
        ),
        sessionToken
    ).wrapErrorMessage()

    internal fun addMobileNumber(
        path: String = apiPath + NABU_PUT_MOBILE,
        mobileNumber: String,
        sessionToken: String
    ): Completable = service.addMobileNumber(
        path,
        AddMobileNumberRequest(mobileNumber),
        sessionToken
    ).wrapErrorMessage()

    internal fun verifyMobileNumber(
        path: String = apiPath + NABU_VERIFICAITIONS,
        mobileNumber: String,
        verificationCode: String,
        sessionToken: String
    ): Completable = service.verifyMobileNumber(
        path,
        MobileVerificationRequest(mobileNumber, verificationCode),
        sessionToken
    ).wrapErrorMessage()

    companion object {
        internal const val CLIENT_TYPE = "APP"
    }
}