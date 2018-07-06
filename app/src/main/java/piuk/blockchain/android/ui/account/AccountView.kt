package piuk.blockchain.android.ui.account

import android.content.Intent
import android.support.annotation.StringRes
import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface AccountView : View {

    val locale: Locale

    fun onShowTransferableLegacyFundsWarning(isAutoPopup: Boolean)

    fun onSetTransferLegacyFundsMenuItemVisible(visible: Boolean)

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun broadcastIntent(intent: Intent)

    fun showWatchOnlyWarningDialog(address: String)

    fun showRenameImportedAddressDialog(address: LegacyAddress)

    fun startScanForResult()

    fun showBip38PasswordDialog(data: String)

    fun updateAccountList(displayAccounts: List<AccountItem>)

    fun hideCurrencyHeader()
}
