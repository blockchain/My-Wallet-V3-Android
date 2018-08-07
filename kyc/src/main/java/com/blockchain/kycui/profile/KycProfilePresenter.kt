package com.blockchain.kycui.profile

import com.blockchain.kycui.profile.models.ProfileModel
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import javax.inject.Inject
import kotlin.properties.Delegates

class KycProfilePresenter @Inject constructor() : BasePresenter<KycProfileView>() {

    var firstNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var lastNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var dateSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }

    override fun onViewReady() = Unit

    internal fun onContinueClicked() {
        check(!view.firstName.isEmpty()) { "firstName is empty" }
        check(!view.lastName.isEmpty()) { "lastName is empty" }

        ProfileModel(
            view.firstName,
            view.lastName,
            view.dateOfBirth ?: throw IllegalStateException("DoB has not been set")
        ).run { view.continueSignUp(this) }
    }

    private fun enableButtonIfComplete() {
        view.setButtonEnabled(firstNameSet && lastNameSet && dateSet)
    }
}