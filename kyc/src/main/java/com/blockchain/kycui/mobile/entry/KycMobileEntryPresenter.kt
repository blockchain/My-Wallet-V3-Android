package com.blockchain.kycui.mobile.entry

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kyc.models.nabu.mapFromMetadata
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycMobileEntryPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager,
    private val settingsDataManager: SettingsDataManager
) : BasePresenter<KycMobileEntryView>() {

    private val fetchOfflineToken by unsafeLazy {
        metadataManager.fetchMetadata(NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE)
            .map {
                it.get()
                    .toMoshiKotlinObject<NabuCredentialsMetadata>()
                    .mapFromMetadata()
            }
            .subscribeOn(Schedulers.io())
            .singleOrError()
            .cache()
    }

    override fun onViewReady() {
        preFillPhoneNumber()
        subscribeToClickEvents()
    }

    private fun subscribeToClickEvents() {
        compositeDisposable +=
            view.phoneNumberObservable
                .retry()
                .map { it.first.sanitized }
                .flatMapCompletable { number: String ->
                    fetchOfflineToken.flatMapCompletable { tokenResponse ->
                        nabuDataManager.addMobileNumber(tokenResponse, number)
                            .subscribeOn(Schedulers.io())
                    }.observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            if (it is NabuApiException &&
                                it.getErrorCode() == NabuErrorCodes.AlreadyRegistered
                            ) {
                                view.displayErrorDialog(R.string.kyc_phone_number_error_conflict)
                            } else {
                                view.showErrorToast(R.string.kyc_phone_number_error_saving_number)
                            }
                        }
                        .doOnComplete { view.continueSignUp() }
                }
                .doOnError(Timber::e)
                .retry()
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
