package piuk.blockchain.android.ui.buysell.coinify.signup.verifyemail

import piuk.blockchain.androidcoreui.ui.base.View

interface CoinifyVerifyEmailView : View {

    fun onStartSignUpSuccess()

    fun onShowVerifiedEmail(emailAddress: String)

    fun onShowUnverifiedEmail(emailAddress: String)

    fun onShowErrorAndClose()

    fun onEnableContinueButton(emailVerified: Boolean)

    fun showLoading(loading: Boolean)

    fun showErrorDialog(errorDescription: String)
}