package piuk.blockchain.android.simplebuy

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.base.FlowFragment
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

interface SimpleBuyScreen : SlidingModalBottomDialog.Host, FlowFragment {
    fun navigator(): SimpleBuyNavigator

    override fun onSheetClosed() {}
}

interface SimpleBuyNavigator : SlidingModalBottomDialog.Host, SmallSimpleBuyNavigator {
    fun goToBuyCryptoScreen(
        addToBackStack: Boolean = true,
        preselectedCrypto: CryptoCurrency,
        preselectedPaymentMethodId: String?
    )
    fun goToCheckOutScreen(addToBackStack: Boolean = true)
    fun goToKycVerificationScreen(addToBackStack: Boolean = true)
    fun goToPendingOrderScreen()
    fun startKyc()
    fun pop()
    fun hasMoreThanOneFragmentInTheStack(): Boolean
    fun goToPaymentScreen(addToBackStack: Boolean = true, isPaymentAuthorised: Boolean = false)
    fun launchIntro()
    fun launchBankAuthWithError(errorState: ErrorState)
}

interface SmallSimpleBuyNavigator {
    fun exitSimpleBuyFlow()
}