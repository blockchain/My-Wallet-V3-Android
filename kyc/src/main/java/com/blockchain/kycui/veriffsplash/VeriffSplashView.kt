package com.blockchain.kycui.veriffsplash

import android.support.annotation.StringRes
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface VeriffSplashView : View {

    val uiState: Observable<String>

    fun continueToVeriff(
        applicant: VeriffApplicantAndToken
    )

    fun showProgressDialog(cancelable: Boolean)

    fun dismissProgressDialog()

    fun continueToCompletion()

    fun showErrorToast(@StringRes message: Int)
}