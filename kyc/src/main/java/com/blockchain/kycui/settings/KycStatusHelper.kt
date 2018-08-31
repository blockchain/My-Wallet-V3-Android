package com.blockchain.kycui.settings

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kycui.extensions.fetchNabuToken
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class KycStatusHelper(
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager,
    private val settingsDataManager: SettingsDataManager
) {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

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

    @VisibleForTesting
    internal fun isInKycRegion(): Single<Boolean> = Single.zip(
        nabuDataManager.getCountriesList(Scope.Kyc)
            .subscribeOn(Schedulers.io()),
        settingsDataManager.getSettings()
            .subscribeOn(Schedulers.io())
            .map { it.countryCode }
            .singleOrError(),
        BiFunction { countries: List<NabuCountryResponse>, countryCode: String ->
            countries
                .map { it.code }
                .contains(countryCode)
        }
    )

    @VisibleForTesting
    internal fun hasAccount(): Single<Boolean> = fetchOfflineToken
        .map { true }
        .onErrorReturn { false }

    @VisibleForTesting
    internal fun shouldDisplayKyc(): Single<Boolean> = Single.zip(
        isInKycRegion(),
        hasAccount(),
        BiFunction { allowedRegion, hasAccount -> allowedRegion || hasAccount }
    )

    @VisibleForTesting
    internal fun getKycStatus(): Single<KycState> = fetchOfflineToken
        .flatMap {
            nabuDataManager.getUser(it)
                .subscribeOn(Schedulers.io())
        }
        .map { it.kycState }
        .onErrorReturn { KycState.None }
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