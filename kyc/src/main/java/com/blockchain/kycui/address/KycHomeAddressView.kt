package com.blockchain.kycui.address

import android.support.annotation.StringRes
import com.blockchain.kycui.address.models.AddressModel
import com.blockchain.kycui.profile.models.ProfileModel
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycHomeAddressView : View {

    val profileModel: ProfileModel

    val address: Observable<AddressModel>

    fun setButtonEnabled(enabled: Boolean)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueToMobileVerification(countryCode: String)

    fun finishPage()

    fun continueToOnfidoSplash(countryCode: String)

    fun continueToTier2MoreInfoNeeded(countryCode: String)

    fun tier1Complete()

    fun restoreUiState(
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryName: String
    )
}
