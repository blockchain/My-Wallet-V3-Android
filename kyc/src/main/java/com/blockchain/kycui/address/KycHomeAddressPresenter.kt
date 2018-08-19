package com.blockchain.kycui.address

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.mapFromMetadata
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.SortedMap

class KycHomeAddressPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager
) : BasePresenter<KycHomeAddressView>() {

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

    override fun onViewReady() {
        compositeDisposable += view.address
            .subscribeBy(
                onNext = {
                    enableButtonIfComplete(it.firstLine, it.city, it.postCode)
                },
                onError = {
                    Timber.e(it)
                    // This is fatal - back out and allow the user to try again
                    view.finishPage()
                }
            )
    }

    internal fun onContinueClicked() {
        compositeDisposable += view.address
            .firstOrError()
            .flatMapCompletable { address ->
                fetchOfflineToken
                    .flatMapCompletable {
                        nabuDataManager.addAddress(
                            it,
                            address.firstLine,
                            address.secondLine,
                            address.city,
                            address.state,
                            address.postCode,
                            address.country
                        ).subscribeOn(Schedulers.io())
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnTerminate { view.dismissProgressDialog() }
            .doOnError(Timber::e)
            .subscribeBy(
                onComplete = { view.continueSignUp() },
                onError = { view.showErrorToast(R.string.kyc_address_error_saving) }
            )
    }

    private fun enableButtonIfComplete(firstLine: String, city: String, zipCode: String) {
        view.setButtonEnabled(!firstLine.isEmpty() && !city.isEmpty() && !zipCode.isEmpty())
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }
}
