package piuk.blockchain.android.ui.buysell.coinify.signup

import android.support.v4.app.Fragment
import piuk.blockchain.androidcoreui.ui.base.View

interface CoinifySignupView : View {

    fun onStartWelcome()

    fun onStartSelectCountry()

    fun onStartVerifyEmail(countryCode: String)

    fun onStartCreateAccountCompleted()

    fun onStartVerifyIdentification(redirectUrl: String, externalKycId: String)

    fun onStartOverview()

    fun onStartInvalidCountry()

    fun onProgressUpdate(currentFragment: Fragment)

    fun showToast(errorDescription: String)

    fun onFinish()

    fun onStartReviewInProgress()
}