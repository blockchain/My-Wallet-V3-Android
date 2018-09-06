package com.blockchain.kycui.navhost

import android.support.annotation.VisibleForTesting
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.extensions.fetchNabuToken
import com.blockchain.kycui.profile.models.ProfileModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycNavHostPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager
) : BasePresenter<KycNavHostView>() {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

    override fun onViewReady() {
        compositeDisposable +=
            fetchOfflineToken.flatMap {
                nabuDataManager.getUser(it)
                    .subscribeOn(Schedulers.io())
            }.observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.displayLoading(true) }
                .subscribeBy(
                    onSuccess = { redirectUserFlow(it) },
                    onError = {
                        Timber.e(it)
                        if (it is MetadataNotFoundException) {
                            // No user, hide loading and start full KYC flow
                            view.displayLoading(false)
                        } else {
                            view.showErrorToastAndFinish(R.string.kyc_status_error)
                        }
                    }
                )
    }

    private fun redirectUserFlow(user: NabuUser) {
        if (user.kycState != KycState.None) {
            // User has completed KYC but not confirmed, proceed to status page
            view.navigateToStatus()
        } else {
            if (user.state == UserState.Active) {
                // All data is present and mobile verified, proceed to Onfido splash
                view.navigateToOnfido(user.toProfileModel(), user.address!!.countryCode!!)
            } else if (user.state == UserState.Created && user.address?.countryCode != null && user.mobile != null) {
                // User backed out at phone number, proceed to phone entry
                view.navigateToMobileEntry(user.toProfileModel(), user.address.countryCode)
            } else if (user.state == UserState.Created && user.address?.countryCode != null) {
                // Address has been entered, skip forward to address
                view.navigateToAddress(user.toProfileModel(), user.address.countryCode)
            } else if (user.state == UserState.Created && user.address?.countryCode == null) {
                // Only profile data has been entered, skip to county code
                view.navigateToCountrySelection()
            }

            // If no other methods are triggered, this will start KYC from scratch. If others have been called,
            // this will make the host fragment visible.
            view.displayLoading(false)
        }
    }
}

@VisibleForTesting
internal fun NabuUser.toProfileModel(): ProfileModel = ProfileModel(
    firstName ?: throw IllegalStateException("First Name is null"),
    lastName ?: throw IllegalStateException("Last Name is null"),
    address?.countryCode ?: throw IllegalStateException("Country Code is null")
)