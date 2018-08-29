package com.blockchain.kyc.datamanagers.nabu

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.services.wallet.RetailWalletTokenService
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.utils.Optional
import info.blockchain.wallet.exceptions.ApiException
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class NabuDataManager(
    private val nabuService: NabuService,
    private val retailWalletTokenService: RetailWalletTokenService,
    private val nabuTokenStore: NabuSessionTokenStore,
    private val appVersion: String,
    private val deviceId: String,
    private val settingsDataManager: SettingsDataManager,
    private val payloadDataManager: PayloadDataManager
) {

    private val guid
        get() = payloadDataManager.guid
    private val sharedKey
        get() = payloadDataManager.sharedKey
    private val emailSingle
        get() = settingsDataManager.getSettings()
            .map { it.email }
            .singleOrError()

    internal fun requestJwt(): Single<String> =
        retailWalletTokenService.requestJwt(
            guid = guid,
            sharedKey = sharedKey
        ).map {
            if (it.isSuccessful) {
                return@map it.token!!
            } else {
                throw ApiException(it.error)
            }
        }

    internal fun getAuthToken(jwt: String): Single<NabuOfflineTokenResponse> =
        nabuService.getAuthToken(jwt = jwt)

    @VisibleForTesting
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
            nabuService.createBasicUser(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                sessionToken = session.token
            ).toSingleDefault(Any())
        }.ignoreElement()

    internal fun getUser(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) { session ->
            nabuService.getUser(sessionToken = session.token)
        }

    internal fun updateUserWalletInfo(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) { session ->
            nabuService.updateWalletInformation(sessionToken = session.token, jwt = jwt)
        }

    internal fun addAddress(
        offlineTokenResponse: NabuOfflineTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.addAddress(
            city = city,
            line1 = line1,
            line2 = line2,
            state = state,
            postCode = postCode,
            countryCode = countryCode,
            sessionToken = it.token
        ).toSingleDefault(Any())
    }.ignoreElement()

    internal fun getOnfidoApiKey(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<String> = authenticate(offlineTokenResponse) {
        nabuService.getOnfidoApiKey(sessionToken = it.token)
    }

    internal fun submitOnfidoVerification(
        offlineTokenResponse: NabuOfflineTokenResponse,
        applicantId: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.submitOnfidoVerification(
            sessionToken = it.token,
            applicantId = applicantId
        ).toSingleDefault(Any())
    }.ignoreElement()

    /**
     * Invalidates the [NabuSessionTokenStore] so that on logging out or switching accounts, no data
     * is persisted accidentally.
     */
    fun clearAccessToken() {
        nabuTokenStore.invalidate()
    }

    internal fun getCountriesList(scope: Scope): Single<List<NabuCountryResponse>> =
        nabuService.getCountriesList(scope = scope)

    private fun unauthenticated(it: Throwable) =
        (it as? NabuApiException?)?.getErrorCode() == NabuErrorCodes.TokenExpired

    // TODO: Refactor this logic into a reusable, thoroughly tested class - see AND-1335
    internal fun <T> authenticate(
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T> =
        if (nabuTokenStore.requiresRefresh()) {
            refreshToken(offlineToken)
        } else {
            nabuTokenStore.getAccessToken()
                .map { (it as Optional.Some).element }
                .singleOrError()
        }.flatMap { tokenResponse ->
            singleFunction(tokenResponse)
                .onErrorResumeNext { refreshOrReturnError(it, offlineToken, singleFunction) }
        }

    private fun <T> refreshOrReturnError(
        throwable: Throwable,
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): SingleSource<out T> {
        return if (unauthenticated(throwable)) {
            refreshToken(offlineToken)
                .doOnSubscribe { clearAccessToken() }
                .flatMap { singleFunction(it) }
        } else {
            Single.error(throwable)
        }
    }

    private fun refreshToken(
        offlineToken: NabuOfflineTokenResponse
    ): Single<NabuSessionTokenResponse> =
        getSessionToken(offlineToken)
            .subscribeOn(Schedulers.io())
            .flatMapObservable(nabuTokenStore::store)
            .singleOrError()
}
