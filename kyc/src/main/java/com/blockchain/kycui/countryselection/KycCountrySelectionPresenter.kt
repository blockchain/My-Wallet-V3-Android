package com.blockchain.kycui.countryselection

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber
import javax.inject.Inject

class KycCountrySelectionPresenter @Inject constructor(
    private val nabuDataManager: NabuDataManager
) : BasePresenter<KycCountrySelectionView>() {

    override fun onViewReady() = Unit

    internal fun onCountrySelected(countryCode: String) {
        nabuDataManager.isInEeaCountry(countryCode, "")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgress() }
            .doOnEvent { _, _ -> view.hideProgress() }
            .subscribeBy(
                onSuccess = { isEea ->
                    if (isEea) {
                        view.continueFlow()
                    } else {
                        view.invalidCountry()
                    }
                },
                onError = {
                    Timber.e(it)
                    view.showErrorToast(R.string.kyc_country_selection_connection_error)
                }
            )
    }

    internal fun onRequestCancelled() {
        compositeDisposable.clear()
    }
}