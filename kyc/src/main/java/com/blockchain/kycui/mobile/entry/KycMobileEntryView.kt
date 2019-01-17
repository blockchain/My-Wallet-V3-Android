package com.blockchain.kycui.mobile.entry

import android.support.annotation.StringRes
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycMobileEntryView : View {

    val uiStateObservable: Observable<Pair<PhoneNumber, Unit>>

    fun preFillPhoneNumber(phoneNumber: String)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp(displayModel: PhoneDisplayModel)
}
