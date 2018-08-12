package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.NABU_COUNTRIES
import com.blockchain.kyc.api.nabu.NABU_CREATE_USER
import com.blockchain.kyc.api.nabu.NABU_INITIAL_AUTH
import com.blockchain.kyc.api.nabu.NABU_SESSION_TOKEN
import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.extensions.wrapErrorMessage
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.models.nabu.NewUserRequest
import com.blockchain.kyc.models.nabu.UserId
import io.reactivex.Single
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Retrofit

class NabuService(
    val environmentConfig: EnvironmentConfig,
    retrofit: Retrofit
) {

    private val service: Nabu = retrofit.create(Nabu::class.java)
    private val apiPath = environmentConfig.apiUrl

    internal fun createUser(
        path: String = apiPath + NABU_CREATE_USER,
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

    internal fun getEeaCountries(
        path: String = apiPath + NABU_COUNTRIES
    ): Single<List<NabuCountryResponse>> = service.getHomebrewRegions(
        path,
        "eea"
    ).wrapErrorMessage()

    companion object {
        internal const val CLIENT_TYPE = "APP"
    }
}