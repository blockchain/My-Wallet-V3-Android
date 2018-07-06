package piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview

import piuk.blockchain.androidcoreui.ui.base.View

interface CoinifyIdentityInReviewView : View {

    fun onShowLoading()

    fun dismissLoading()

    fun onFinish()

    fun onShowCompleted()

    fun onShowReviewing()

    fun onShowPending()

    fun onShowRejected()

    fun onShowExpired()

    fun onShowFailed()

    fun onShowDocumentsRequested()
}