package com.blockchain.kycui.profile

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata.Companion.USER_CREDENTIALS_METADATA_NODE
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.mapFromMetadata
import com.blockchain.kyc.models.nabu.mapToMetadata
import com.blockchain.kyc.util.toSimpleDateString
import com.blockchain.kycui.profile.models.ProfileModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlin.properties.Delegates

class KycProfilePresenter(
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager
) : BasePresenter<KycProfileView>() {

    var firstNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var lastNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var dateSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }

    override fun onViewReady() = Unit

    internal fun onContinueClicked() {
        check(!view.firstName.isEmpty()) { "firstName is empty" }
        check(!view.lastName.isEmpty()) { "lastName is empty" }
        check(view.dateOfBirth != null) { "dateOfBirth is null" }

        metadataManager.fetchMetadata(USER_CREDENTIALS_METADATA_NODE)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { optionalToken ->
                if (optionalToken.isPresent) {
                    createBasicUser(
                        optionalToken.get()
                            .toMoshiKotlinObject<NabuCredentialsMetadata>()
                            .mapFromMetadata()
                    )
                } else {
                    createUserAndStoreInMetadata()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(Timber::e)
            .doOnSubscribe { view.showProgressDialog() }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onComplete = {
                    ProfileModel(
                        view.firstName,
                        view.lastName,
                        view.dateOfBirth ?: throw IllegalStateException("DoB has not been set")
                    ).run { view.continueSignUp(this) }
                },
                onError = { view.showErrorToast(R.string.kyc_profile_error) }
            )
    }

    private fun createUserAndStoreInMetadata(): Completable = nabuDataManager.createUserId()
        .subscribeOn(Schedulers.io())
        .flatMapCompletable { userId ->
            nabuDataManager.getAuthToken(userId)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { tokenResponse ->
                    metadataManager.saveToMetadata(tokenResponse.mapToMetadata())
                        .toSingle { tokenResponse }
                        .flatMapCompletable { createBasicUser(it) }
                }
        }

    private fun createBasicUser(offlineToken: NabuOfflineTokenResponse): Completable =
        nabuDataManager.createBasicUser(
            view.firstName,
            view.lastName,
            view.dateOfBirth?.toSimpleDateString()
                ?: throw IllegalStateException("DoB has not been set"),
            offlineToken
        ).subscribeOn(Schedulers.io())

    private fun enableButtonIfComplete() {
        view.setButtonEnabled(firstNameSet && lastNameSet && dateSet)
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }
}