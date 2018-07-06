package piuk.blockchain.android.ui.ssl

import piuk.blockchain.androidcore.utils.SSLVerifyUtil
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import javax.inject.Inject

class SSLVerifyPresenter @Inject constructor(
    private val sslVerifyUtil: SSLVerifyUtil
) : BasePresenter<SSLVerifyView>() {

    override fun onViewReady() {
        view.showWarningPrompt()
    }

    fun validateSSL() {
        sslVerifyUtil.validateSSL()
    }
}