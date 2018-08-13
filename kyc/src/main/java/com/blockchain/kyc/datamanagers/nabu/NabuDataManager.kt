package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.stores.NabuSessionTokenStore
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.Optional

class NabuDataManager(
    private val nabuService: NabuService,
    private val nabuTokenStore: NabuSessionTokenStore,
    private val appVersion: String,
    private val deviceId: String,
    private val settingsDataManager: SettingsDataManager,
    payloadDataManager: PayloadDataManager
) {

    private val guid = payloadDataManager.guid
    private val emailSingle
        get() = settingsDataManager.getSettings()
            .map { it.email }
            .singleOrError()

    internal fun createUserId(): Single<String> =
        emailSingle.flatMap { email ->
            nabuService.createUserId(
                guid = guid,
                email = email
            ).map { it.userId }
        }

    internal fun getAuthToken(userId: String): Single<NabuOfflineTokenResponse> =
        emailSingle.flatMap {
            nabuService.getAuthToken(
                guid = guid,
                email = it,
                userId = userId,
                deviceId = deviceId,
                appVersion = appVersion
            )
        }

    internal fun getSessionToken(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuSessionTokenResponse> =
        emailSingle.flatMap {
            nabuService.getSessionToken(
                userId = offlineTokenResponse.userId,
                offlineToken = offlineTokenResponse.token,
                guid = guid,
                email = it,
                deviceId = deviceId,
                appVersion = appVersion
            )
        }

    internal fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Completable =
        authenticate(offlineTokenResponse) { session ->
            emailSingle.flatMapCompletable { email ->
                nabuService.createBasicUser(
                    userId = session.userId,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    dateOfBirth = dateOfBirth,
                    sessionToken = session.token
                )
            }.toSingleDefault(Any())
        }.ignoreElement()

    internal fun getUser(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) { session ->
            nabuService.getUser(
                userId = session.userId,
                sessionToken = session.token
            )
        }

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

    private fun <T> authenticate(
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T> =
        if (nabuTokenStore.requiresRefresh()) {
            refreshToken(offlineToken)
        } else {
            nabuTokenStore.getAccessToken()
                .map { (it as Optional.Some).element }
                .singleOrError()
        }.flatMap {
            singleFunction(it)
                .onErrorResumeNext(refreshTokenAndRetry(offlineToken, singleFunction))
        }

    private fun refreshToken(
        offlineToken: NabuOfflineTokenResponse
    ): Single<NabuSessionTokenResponse> =
        getSessionToken(offlineToken)
            .subscribeOn(Schedulers.io())
            .flatMapObservable(nabuTokenStore::store)
            .singleOrError()

    private fun <T> refreshTokenAndRetry(
        offlineToken: NabuOfflineTokenResponse,
        singleToResume: (NabuSessionTokenResponse) -> Single<T>
    ): Function<Throwable, out Single<T>> =
        Function {
            if (unauthenticated(it)) {
                clearAccessToken()
                return@Function refreshToken(offlineToken)
                    .flatMap { singleToResume(it) }
            } else {
                return@Function Single.error(it)
            }
        }
}