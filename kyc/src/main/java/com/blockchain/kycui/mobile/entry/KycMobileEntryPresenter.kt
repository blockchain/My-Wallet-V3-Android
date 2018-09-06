package com.blockchain.kycui.mobile.entry

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kycui.extensions.fetchNabuToken
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycMobileEntryPresenter(
    private val settingsDataManager: SettingsDataManager,
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager
) : BasePresenter<KycMobileEntryView>() {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

    override fun onViewReady() {
        preFillPhoneNumber()
        subscribeToClickEvents()
    }

    private fun subscribeToClickEvents() {
        compositeDisposable +=
            view.uiStateObservable
                .map { it.first }
                .flatMapCompletable { number ->
                    settingsDataManager.updateSms(number.sanitized)
                        .flatMapCompletable { _ ->
                            nabuDataManager.requestJwt()
                                .subscribeOn(Schedulers.io())
                                .flatMap { jwt ->
                                    fetchOfflineToken.flatMap {
                                        nabuDataManager.updateUserWalletInfo(it, jwt)
                                            .subscribeOn(Schedulers.io())
                                    }
                                }
                                .ignoreElement()
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError { view.showErrorToast(R.string.kyc_phone_number_error_saving_number) }
                        .doOnComplete {
                            view.continueSignUp(PhoneDisplayModel(number.raw, number.sanitized))
                        }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    private fun preFillPhoneNumber() {
        compositeDisposable +=
            settingsDataManager.getSettings()
                .map { it.smsNumber ?: "" }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        if (!it.isEmpty() && it.first() == '+') {
                            view.preFillPhoneNumber(it)
                        }
                    },
                    // Ignore error
                    onError = { Timber.e(it) }
                )
    }

    internal fun onProgressCancelled() {
        // Cancel outbound
        compositeDisposable.clear()
        // Resubscribe to everything
        subscribeToClickEvents()
    }
}
