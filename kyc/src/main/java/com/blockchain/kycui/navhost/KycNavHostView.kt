package com.blockchain.kycui.navhost

import android.support.annotation.StringRes
import com.blockchain.kycui.profile.models.ProfileModel
import piuk.blockchain.androidcoreui.ui.base.View

interface KycNavHostView : View {

    fun displayLoading(loading: Boolean)

    fun navigateToStatus()

    fun showErrorToastAndFinish(@StringRes message: Int)

    fun navigateToProfile(countryCode: String)

    fun navigateToCountrySelection()

    fun navigateToAddress(profileModel: ProfileModel, countryCode: String)

    fun navigateToMobileEntry(profileModel: ProfileModel, countryCode: String)

    fun navigateToOnfido(profileModel: ProfileModel, countryCode: String)
}