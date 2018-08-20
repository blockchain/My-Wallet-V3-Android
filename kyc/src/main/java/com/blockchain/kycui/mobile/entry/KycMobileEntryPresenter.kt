package com.blockchain.kycui.mobile.entry

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
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
                    onError = {
                        // Ignore error
                        Timber.e(it)
                    }
                )

        compositeDisposable +=
            view.phoneNumber
                .doOnError(Timber::e)
                .subscribeBy(
                    onNext = { enableButtonIfComplete(it.sanitizePhoneNumber()) },
                    onError = { view.finishPage() }
                )
    }

    internal fun onContinueClicked() {
        compositeDisposable +=
            view.phoneNumber
                .firstOrError()
                .map { it.sanitizePhoneNumber() }
                .flatMapCompletable { number: String ->
                    fetchOfflineToken.flatMapCompletable {
                        nabuDataManager.addMobileNumber(it, number)
                            .subscribeOn(Schedulers.io())
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .subscribeBy(
                    onComplete = { view.continueSignUp() },
                    onError = {
                        view.showErrorToast(R.string.kyc_phone_number_error_saving_number)
                    }
                )
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }

    private fun enableButtonIfComplete(phoneNumber: String) {
        // 5 is the minimum phone number length + area code + "+" symbol
        view.setButtonEnabled(phoneNumber.length >= 9)
    }

    private fun String.sanitizePhoneNumber(): String = "+${this.replace("[^\\d.]".toRegex(), "")}"
}
