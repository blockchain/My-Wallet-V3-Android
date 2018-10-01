package com.blockchain.kycui.settings

import android.support.annotation.VisibleForTesting
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.extensions.fetchNabuToken
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrefsUtil
import timber.log.Timber

class KycStatusHelper(
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager,
    private val settingsDataManager: SettingsDataManager,
    private val prefsUtil: PrefsUtil
) {

    private val fetchOfflineToken
        get() = metadataManager.fetchNabuToken()

    fun getSettingsKycState(): Single<SettingsKycState> = Single.zip(
        shouldDisplayKyc(),
        getKycStatus(),
        BiFunction { shouldDisplay, status ->
            if (!shouldDisplay) {
                SettingsKycState.Hidden
            } else {
                status.toUiState()
            }
        }
    )

    fun shouldDisplayKyc(): Single<Boolean> = Single.zip(
        isInKycRegion(),
        hasAccount(),
        BiFunction { allowedRegion, hasAccount -> allowedRegion || hasAccount }
    )

    fun syncPhoneNumberWithNabu(): Completable = nabuDataManager.requestJwt()
        .subscribeOn(Schedulers.io())
        .flatMap { jwt ->
            fetchOfflineToken.flatMap {
                nabuDataManager.updateUserWalletInfo(it, jwt)
                    .subscribeOn(Schedulers.io())
            }
        }
        .ignoreElement()
        .doOnError { Timber.e(it) }
        .onErrorResumeNext {
            if (it is MetadataNotFoundException) {
                // Allow users who aren't signed up to Nabu to ignore this failure
                return@onErrorResumeNext Completable.complete()
            } else {
                return@onErrorResumeNext Completable.error { it }
            }
        }

    fun getKycStatus(): Single<KycState> = fetchOfflineToken
        .flatMap {
            nabuDataManager.getUser(it)
                .subscribeOn(Schedulers.io())
        }
        .map { it.kycState }
        .doOnError { Timber.e(it) }
        .onErrorReturn { KycState.None }

    fun getUserState(): Single<UserState> =
        fetchOfflineToken
            .flatMap {
                nabuDataManager.getUser(it)
                    .subscribeOn(Schedulers.io())
            }
            .map { it.state }
            .doOnError { Timber.e(it) }
            .onErrorReturn { UserState.None }

    @VisibleForTesting
    internal fun hasAccount(): Single<Boolean> = fetchOfflineToken
        .map { true }
        .onErrorReturn { false }

    @VisibleForTesting
    internal fun isInKycRegion(): Single<Boolean> = Single.zip(
        isStoredCountryInKycRegion(),
        isBestGuessCountryInKycRegion(),
        BiFunction { stored: Boolean, current: Boolean -> stored || current }
    )

    private fun isStoredCountryInKycRegion(): Single<Boolean> =
        isInKycRegion(prefsUtil.getValue(PersistentPrefs.PREF_CHOSEN_KYC_COUNTRY, null))

    private fun isBestGuessCountryInKycRegion(): Single<Boolean> =
        settingsDataManager.getSettings()
            .subscribeOn(Schedulers.io())
            .map { it.countryCode }
            .flatMapSingle { isInKycRegion(it) }
            .singleOrError()

    private fun isInKycRegion(countryCode: String?): Single<Boolean> =
        nabuDataManager.getCountriesList(Scope.Kyc)
            .subscribeOn(Schedulers.io())
            .map { countries ->
                countries.asSequence()
                    .map { it.code }
                    .contains(countryCode)
            }
}

private fun KycState.toUiState(): SettingsKycState = when (this) {
    KycState.None -> SettingsKycState.Unverified
    KycState.Pending -> SettingsKycState.InProgress
    KycState.UnderReview -> SettingsKycState.UnderReview
    KycState.Rejected -> SettingsKycState.Failed
    KycState.Expired -> SettingsKycState.Failed
    KycState.Verified -> SettingsKycState.Verified
}

enum class SettingsKycState {
    Unverified,
    Verified,
    InProgress,
    UnderReview,
    Failed,
    Hidden
}