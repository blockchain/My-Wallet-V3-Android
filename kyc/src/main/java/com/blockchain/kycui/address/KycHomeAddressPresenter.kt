package com.blockchain.kycui.address

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.mapFromMetadata
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import kotlin.properties.Delegates

class KycHomeAddressPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager
) : BasePresenter<KycHomeAddressView>() {

    var firstLineSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var citySet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var zipCodeSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }

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

    override fun onViewReady() = Unit

    internal fun onContinueClicked() {
        fetchOfflineToken
            .flatMapCompletable {
                nabuDataManager.addAddress(
                    it,
                    view.firstLine,
                    view.secondLine,
                    view.city,
                    view.state,
                    view.zipCode,
                    view.country
                ).subscribeOn(Schedulers.io())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    Timber.d("Address successfully added")
                },
                onError = {
                    Timber.e(it)
                }
            )
    }

    private fun enableButtonIfComplete() {
        view.setButtonEnabled(firstLineSet && citySet && zipCodeSet)
    }


    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }
}
