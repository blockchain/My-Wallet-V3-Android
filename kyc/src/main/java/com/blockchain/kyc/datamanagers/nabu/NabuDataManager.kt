package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.stores.NabuSessionTokenStore
import io.reactivex.Single

class NabuDataManager(
    private val nabuService: NabuService,
    private val nabuTokenStore: NabuSessionTokenStore,
    private val appVersion: String,
    private val deviceId: String
) {

    fun createUser(
        guid: String,
        email: String
    ): Single<String> = nabuService.createUser(
        guid = guid,
        email = email
    ).map { it.userId }

    fun getAuthToken(
        guid: String,
        email: String,
        userId: String
    ): Single<NabuOfflineTokenResponse> = nabuService.getAuthToken(
        guid = guid,
        email = email,
        userId = userId,
        deviceId = deviceId,
        appVersion = appVersion
    )

    fun getSessionToken(
        guid: String,
        email: String,
        offlineToken: String,
        userId: String
    ): Single<NabuSessionTokenResponse> = nabuService.getSessionToken(
        userId = userId,
        offlineToken = offlineToken,
        guid = guid,
        email = email,
        deviceId = deviceId,
        appVersion = appVersion
    )

    /**
     * Invalidates the [NabuSessionTokenStore] so that on logging out or switching accounts, no data
     * is persisted accidentally.
     */
    fun clearAccessToken() {
        nabuTokenStore.invalidate()
    }

    internal fun isInEeaCountry(countryCode: String): Single<Boolean> =
        nabuService.getEeaCountries()
            .map { it.containsCountry(countryCode) }

    private fun List<NabuCountryResponse>.containsCountry(countryCode: String): Boolean =
        this.any { it.code.equals(countryCode, ignoreCase = true) }

    private fun unauthenticated(it: Throwable) =
        (it as? NabuApiException?)?.getErrorCode() == NabuErrorCodes.TokenExpired
}