package com.blockchain.kycui.address

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.mapFromMetadata
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.SortedMap
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

    val countryCodeSingle: Single<SortedMap<String, String>> by unsafeLazy {
        fetchOfflineToken
            .flatMap {
                nabuDataManager.getCountriesList(Scope.None)
                    .subscribeOn(Schedulers.io())
            }
            .map { list ->
                list.associateBy({ it.name }, { it.code })
                    .toSortedMap()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .cache()
    }

    override fun onViewReady() = Unit

    internal fun onContinueClicked() {
        check(!view.firstLine.isEmpty()) { "firstLine is empty" }
        check(!view.city.isEmpty()) { "city is empty" }
        check(!view.zipCode.isEmpty()) { "zipCode is empty" }

        fetchOfflineToken
            .flatMapCompletable {
                nabuDataManager.addAddress(
                    it,
                    view.firstLine,
                    view.secondLine,
                    view.city,
                    view.state,
                    view.zipCode,
                    view.countryCode
                ).subscribeOn(Schedulers.io())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(Timber::e)
            .doOnSubscribe { view.showProgressDialog() }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onComplete = { view.continueSignUp() },
                onError = { view.showErrorToast(R.string.kyc_address_error_saving) }
            )
    }

    private fun enableButtonIfComplete() {
        view.setButtonEnabled(firstLineSet && citySet && zipCodeSet)
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }
}
