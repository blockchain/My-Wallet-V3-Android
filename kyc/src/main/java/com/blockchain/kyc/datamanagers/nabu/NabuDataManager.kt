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
        nabuService.getAuthToken(jwt)

    @VisibleForTesting
    internal fun getSessionToken(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuSessionTokenResponse> =
        emailSingle.flatMap {
            nabuService.getSessionToken(
                offlineTokenResponse.userId,
                offlineTokenResponse.token,
                guid,
                it,
                deviceId,
                appVersion
            )
        }

    internal fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Completable =
        authenticate(offlineTokenResponse) {
            nabuService.createBasicUser(
                firstName,
                lastName,
                dateOfBirth,
                it
            ).toSingleDefault(Any())
        }.ignoreElement()

    internal fun getUser(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) {
            nabuService.getUser(it)
        }

    internal fun updateUserWalletInfo(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) {
            nabuService.updateWalletInformation(it, jwt)
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
            it,
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ).toSingleDefault(Any())
    }.ignoreElement()

    internal fun recordCountrySelection(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String,
        countryCode: String,
        notifyWhenAvailable: Boolean
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.recordCountrySelection(
            it,
            jwt,
            countryCode,
            notifyWhenAvailable
        ).toSingleDefault(Any())
    }.ignoreElement()

    internal fun getOnfidoApiKey(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<String> = authenticate(offlineTokenResponse) {
        nabuService.getOnfidoApiKey(it)
    }

    internal fun submitOnfidoVerification(
        offlineTokenResponse: NabuOfflineTokenResponse,
        applicantId: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.submitOnfidoVerification(it, applicantId)
            .toSingleDefault(Any())
    }.ignoreElement()

    /**
     * Invalidates the [NabuSessionTokenStore] so that on logging out or switching accounts, no data
     * is persisted accidentally.
     */
    fun clearAccessToken() {
        nabuTokenStore.invalidate()
    }

    internal fun getCountriesList(scope: Scope): Single<List<NabuCountryResponse>> =
        nabuService.getCountriesList(scope)

    private fun unauthenticated(throwable: Throwable) =
        (throwable as? NabuApiException?)?.getErrorCode() == NabuErrorCodes.TokenExpired

    // TODO: Refactor this logic into a reusable, thoroughly tested class - see AND-1335
    internal fun <T> authenticate(
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T> =
        currentToken(offlineToken).flatMap { tokenResponse ->
            singleFunction(tokenResponse)
                .onErrorResumeNext { refreshOrReturnError(it, offlineToken, singleFunction) }
        }

    internal fun invalidateToken() {
        nabuTokenStore.invalidate()
    }

    fun currentToken(offlineToken: NabuOfflineTokenResponse): Single<NabuSessionTokenResponse> =
        if (nabuTokenStore.requiresRefresh()) {
            refreshToken(offlineToken)
        } else {
            nabuTokenStore.getAccessToken()
                .map { (it as Optional.Some).element }
                .singleOrError()
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
