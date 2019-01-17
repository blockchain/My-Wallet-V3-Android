package com.blockchain.kycui.navhost

import com.blockchain.BaseKycPresenter
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.logging.KycResumedEvent
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.profile.models.ProfileModel
import com.blockchain.kycui.reentry.KycNavigator
import com.blockchain.kycui.reentry.ReentryDecision
import com.blockchain.nabu.NabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycNavHostPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val reentryDecision: ReentryDecision,
    private val kycNavigator: KycNavigator
) : BaseKycPresenter<KycNavHostView>(nabuToken) {

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
        if (user.state != UserState.None && user.kycState == KycState.None) {
            val current = user.tiers?.current
            if (current == null || current == 0) {
                val reentryPoint = reentryDecision.findReentryPoint(user)
                val directions = kycNavigator.userAndReentryPointToDirections(user, reentryPoint)
                view.navigate(directions)
                Logging.logCustom(KycResumedEvent(reentryPoint))
            }
        } else if (view.campaignType == CampaignType.Sunriver) {
            view.navigateToAirdropSplash()
        }

        // If no other methods are triggered, this will start KYC from scratch. If others have been called,
        // this will make the host fragment visible.
        view.displayLoading(false)
    }
}

internal fun NabuUser.toProfileModel(): ProfileModel = ProfileModel(
    firstName ?: throw IllegalStateException("First Name is null"),
    lastName ?: throw IllegalStateException("Last Name is null"),
    address?.countryCode ?: throw IllegalStateException("Country Code is null")
)