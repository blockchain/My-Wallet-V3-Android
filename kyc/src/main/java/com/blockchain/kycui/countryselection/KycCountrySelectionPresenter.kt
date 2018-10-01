package com.blockchain.kycui.countryselection

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kycui.countryselection.models.CountrySelectionState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs.PREF_CHOSEN_KYC_COUNTRY
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycCountrySelectionPresenter(
    private val nabuDataManager: NabuDataManager,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val prefsUtil: PrefsUtil
) : BasePresenter<KycCountrySelectionView>() {

    private val countriesList by unsafeLazy {
        nabuDataManager.getCountriesList(Scope.None)
            .cache()
    }

    override fun onViewReady() {
        compositeDisposable +=
            countriesList
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.renderUiState(CountrySelectionState.Loading) }
                .doOnError {
                    view.renderUiState(
                        CountrySelectionState.Error(R.string.kyc_country_selection_connection_error)
                    )
                }
                .doOnSuccess { view.renderUiState(CountrySelectionState.Data(it)) }
                .subscribeBy(onError = { Timber.e(it) })
    }

    internal fun onCountrySelected(countryCode: String) {
        compositeDisposable +=
            countriesList.filter { it.isKycCountry(countryCode) }
                .subscribeBy(
                    onSuccess = { view.continueFlow(countryCode) },
                    onComplete = { checkShapeShift(countryCode) },
                    onError = {
                        throw IllegalStateException("Countries list should already be cached")
                    }
                )
    }

    private fun checkShapeShift(countryCode: String) {
        compositeDisposable +=
            walletOptionsDataManager.isInShapeShiftCountry(countryCode)
                .subscribeBy(
                    onSuccess = {
                        if (it) {
                            prefsUtil.setValue(PREF_CHOSEN_KYC_COUNTRY, countryCode)
                            view.redirectToShapeShift()
                        } else {
                            view.invalidCountry(countryCode)
                        }
                    }
                )
    }

    private fun List<NabuCountryResponse>.isKycCountry(countryCode: String): Boolean =
        this.any { countryResponse ->
            countryResponse.code.equals(countryCode, ignoreCase = true) &&
                countryResponse.scopes.any { it.equals(Scope.Kyc.value, ignoreCase = true) }
        }

    internal fun onRequestCancelled() {
        compositeDisposable.clear()
    }
}