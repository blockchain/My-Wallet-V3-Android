package com.blockchain.kycui.address

import com.blockchain.BaseKycPresenter
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kycui.address.models.AddressModel
import com.blockchain.nabu.NabuToken
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.settings.PhoneVerificationQuery
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.SortedMap

interface Tier2Decision {

    enum class NextStep {
        Tier1Complete,
        Tier2ContinueTier1NeedsMoreInfo,
        Tier2Continue
    }

    fun progressToTier2(): Single<NextStep>
}

class KycHomeAddressPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val tier2Decision: Tier2Decision,
    private val phoneVerificationQuery: PhoneVerificationQuery
) : BaseKycPresenter<KycHomeAddressView>(nabuToken) {

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

        restoreDataIfPresent()
    }

    private fun restoreDataIfPresent() {
        compositeDisposable +=
            view.address
                .firstElement()
                .flatMap { addressModel ->
                    // Don't attempt to restore state if data is already present
                    if (addressModel.containsData()) {
                        Maybe.empty()
                    } else {
                        fetchOfflineToken
                            .flatMapMaybe { tokenResponse ->
                                nabuDataManager.getUser(tokenResponse)
                                    .subscribeOn(Schedulers.io())
                                    .flatMapMaybe { user ->
                                        user.address?.let { address ->
                                            Maybe.just(address)
                                                .flatMap { getCountryName(address.countryCode!!) }
                                                .map { it to address }
                                        } ?: Maybe.empty()
                                    }
                            }
                            .observeOn(AndroidSchedulers.mainThread())
                    }
                }
                .subscribeBy(
                    onSuccess = { (countryName, address) ->
                        view.restoreUiState(
                            address.line1!!,
                            address.line2,
                            address.city!!,
                            address.state,
                            address.postCode,
                            countryName
                        )
                    },
                    onError = {
                        // Silently fail
                        Timber.e(it)
                    }
                )
    }

    private data class State(
        val phoneVerified: Boolean,
        val progressToTier2: Tier2Decision.NextStep,
        val countryCode: String
    )

    internal fun onContinueClicked() {
        compositeDisposable += view.address
            .firstOrError()
            .flatMap { address ->
                addAddress(address)
                    .andThen(phoneVerificationQuery.isPhoneNumberVerified())
                    .map { verified -> verified to address.country }
            }
            .flatMap { (verified, countryCode) ->
                if (verified) {
                    updateNabuData()
                } else {
                    Completable.complete()
                }.andThen(Single.just(verified to countryCode))
            }
            .map { (verified, countryCode) ->
                State(
                    progressToTier2 = Tier2Decision.NextStep.Tier1Complete,
                    countryCode = countryCode,
                    phoneVerified = verified
                )
            }
            .zipWith(tier2Decision.progressToTier2())
            .map { (x, progress) -> x.copy(progressToTier2 = progress) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError(Timber::e)
            .subscribeBy(
                onSuccess = {
                    when (it.progressToTier2) {
                        Tier2Decision.NextStep.Tier1Complete -> view.tier1Complete()
                        Tier2Decision.NextStep.Tier2ContinueTier1NeedsMoreInfo ->
                            view.continueToTier2MoreInfoNeeded(it.countryCode)
                        Tier2Decision.NextStep.Tier2Continue -> if (it.phoneVerified) {
                            view.continueToOnfidoSplash(it.countryCode)
                        } else {
                            view.continueToMobileVerification(it.countryCode)
                        }
                    }
                },
                onError = { view.showErrorToast(R.string.kyc_address_error_saving) }
            )
    }

    private fun addAddress(address: AddressModel): Completable = fetchOfflineToken
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

    private fun updateNabuData(): Completable = nabuDataManager.requestJwt()
        .subscribeOn(Schedulers.io())
        .flatMap { jwt ->
            fetchOfflineToken.flatMap {
                nabuDataManager.updateUserWalletInfo(it, jwt)
                    .subscribeOn(Schedulers.io())
            }
        }
        .ignoreElement()

    private fun getCountryName(countryCode: String): Maybe<String> = countryCodeSingle
        .map { it.entries.first { (_, value) -> value == countryCode }.key }
        .toMaybe()

    private fun enableButtonIfComplete(firstLine: String, city: String, zipCode: String) {
        view.setButtonEnabled(!firstLine.isEmpty() && !city.isEmpty() && !zipCode.isEmpty())
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }

    private fun AddressModel.containsData(): Boolean =
        !firstLine.isEmpty() ||
            !secondLine.isNullOrEmpty() ||
            !city.isEmpty() ||
            !state.isNullOrEmpty() ||
            !postCode.isEmpty()
}
