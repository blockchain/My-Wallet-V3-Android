package com.blockchain.kyc.datamanagers.nabu

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kyc.models.nabu.NabuStateResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.SupportedDocuments
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

interface NabuDataManager {

    fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Completable

    fun requestJwt(): Single<String>

    fun getUser(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuUser>

    fun getCountriesList(scope: Scope): Single<List<NabuCountryResponse>>

    fun updateUserWalletInfo(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String
    ): Single<NabuUser>

    fun addAddress(
        offlineTokenResponse: NabuOfflineTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable

    fun recordCountrySelection(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable

    fun getOnfidoApiKey(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<String>

    fun submitOnfidoVerification(
        offlineTokenResponse: NabuOfflineTokenResponse,
        applicantId: String
    ): Completable

    fun getStatesList(countryCode: String, scope: Scope): Single<List<NabuStateResponse>>

    fun getSupportedDocuments(
        offlineTokenResponse: NabuOfflineTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>>

    fun registerCampaign(
        offlineTokenResponse: NabuOfflineTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable

    fun getCampaignList(offlineTokenResponse: NabuOfflineTokenResponse): Single<List<String>>

    fun getAuthToken(jwt: String): Single<NabuOfflineTokenResponse>

    fun <T> authenticate(
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T>

    fun clearAccessToken()

    fun invalidateToken()

    fun currentToken(offlineToken: NabuOfflineTokenResponse): Single<NabuSessionTokenResponse>
}

internal class NabuDataManagerImpl(
    private val nabuService: NabuService,
    private val retailWalletTokenService: RetailWalletTokenService,
    private val nabuTokenStore: NabuSessionTokenStore,
    private val appVersion: String,
    private val deviceId: String,
    private val settingsDataManager: SettingsDataManager,
    private val payloadDataManager: PayloadDataManager
) : NabuDataManager {

    private val guid
        get() = payloadDataManager.guid
    private val sharedKey
        get() = payloadDataManager.sharedKey
    private val emailSingle
        get() = settingsDataManager.getSettings()
            .map { it.email }
            .singleOrError()

    override fun requestJwt(): Single<String> =
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

    override fun getAuthToken(jwt: String): Single<NabuOfflineTokenResponse> =
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

    override fun createBasicUser(
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

    override fun getUser(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) {
            nabuService.getUser(it)
        }

    override fun updateUserWalletInfo(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String
    ): Single<NabuUser> =
        authenticate(offlineTokenResponse) {
            nabuService.updateWalletInformation(it, jwt)
        }

    override fun addAddress(
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

    override fun recordCountrySelection(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.recordCountrySelection(
            it,
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        ).toSingleDefault(Any())
    }.ignoreElement()

    override fun getOnfidoApiKey(
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Single<String> = authenticate(offlineTokenResponse) {
        nabuService.getOnfidoApiKey(it)
    }

    override fun submitOnfidoVerification(
        offlineTokenResponse: NabuOfflineTokenResponse,
        applicantId: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.submitOnfidoVerification(it, applicantId)
            .toSingleDefault(Any())
    }.ignoreElement()

    override fun registerCampaign(
        offlineTokenResponse: NabuOfflineTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = authenticate(offlineTokenResponse) {
        nabuService.registerCampaign(it, campaignRequest, campaignName)
            .toSingleDefault(Any())
    }.ignoreElement()

    override fun getCampaignList(offlineTokenResponse: NabuOfflineTokenResponse): Single<List<String>> =
        getUser(offlineTokenResponse)
            .map { it.tags?.keys?.toList() ?: emptyList() }

    /**
     * Invalidates the [NabuSessionTokenStore] so that on logging out or switching accounts, no data
     * is persisted accidentally.
     */
    override fun clearAccessToken() {
        nabuTokenStore.invalidate()
    }

    override fun getCountriesList(scope: Scope): Single<List<NabuCountryResponse>> =
        nabuService.getCountriesList(scope)

    override fun getStatesList(countryCode: String, scope: Scope): Single<List<NabuStateResponse>> =
        nabuService.getStatesList(countryCode, scope)

    override fun getSupportedDocuments(
        offlineTokenResponse: NabuOfflineTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>> = authenticate(offlineTokenResponse) {
        nabuService.getSupportedDocuments(it, countryCode)
    }

    private fun unauthenticated(throwable: Throwable) =
        (throwable as? NabuApiException?)?.getErrorCode() == NabuErrorCodes.TokenExpired

    private fun userRestored(throwable: Throwable) =
        (throwable as? NabuApiException?)?.getErrorCode() == NabuErrorCodes.AlreadyRegistered

    // TODO: Refactor this logic into a reusable, thoroughly tested class - see AND-1335
    override fun <T> authenticate(
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T> =
        currentToken(offlineToken)
            .flatMap { tokenResponse ->
                singleFunction(tokenResponse)
                    .onErrorResumeNext { refreshOrReturnError(it, offlineToken, singleFunction) }
            }

    override fun invalidateToken() {
        nabuTokenStore.invalidate()
    }

    override fun currentToken(offlineToken: NabuOfflineTokenResponse): Single<NabuSessionTokenResponse> =
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
    ): SingleSource<T> =
        if (unauthenticated(throwable)) {
            refreshToken(offlineToken)
                .doOnSubscribe { clearAccessToken() }
                .flatMap { singleFunction(it) }
        } else {
            Single.error(throwable)
        }

    private fun recoverOrReturnError(
        throwable: Throwable,
        offlineToken: NabuOfflineTokenResponse
    ): SingleSource<NabuSessionTokenResponse> =
        if (userRestored(throwable)) {
            recoverUserAndContinue(offlineToken)
        } else {
            Single.error(throwable)
        }

    private fun recoverUserAndContinue(
        offlineToken: NabuOfflineTokenResponse
    ): Single<NabuSessionTokenResponse> =
        requestJwt()
            .flatMapCompletable { nabuService.recoverUser(offlineToken, it) }
            .andThen(refreshToken(offlineToken))

    private fun refreshToken(
        offlineToken: NabuOfflineTokenResponse
    ): Single<NabuSessionTokenResponse> =
        getSessionToken(offlineToken)
            .subscribeOn(Schedulers.io())
            .flatMapObservable(nabuTokenStore::store)
            .singleOrError()
            .onErrorResumeNext { recoverOrReturnError(it, offlineToken) }
}
