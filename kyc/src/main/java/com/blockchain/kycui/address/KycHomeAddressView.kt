package com.blockchain.kycui.address

import android.support.annotation.StringRes
import com.blockchain.kycui.profile.models.ProfileModel
import piuk.blockchain.androidcoreui.ui.base.View

interface KycHomeAddressView : View {

    val profileModel: ProfileModel

    val firstLine: String

    val secondLine: String

    val city: String

    val state: String

    val zipCode: String

    var countryCode: String

    fun setButtonEnabled(enabled: Boolean)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp()
}
