package piuk.blockchain.android.ui.send

import android.support.annotation.ColorRes
import android.support.annotation.Nullable
import android.support.annotation.StringRes
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.androidcore.data.currency.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface SendView : View {

    val locale: Locale

    // Update field
    fun updateSendingAddress(label: String)

    fun updateReceivingHint(hint: Int)

    fun updateCryptoCurrency(currency: String)

    fun updateFiatCurrency(currency: String)

    fun updateCryptoAmount(amountString: String?)

    fun updateFiatAmount(amountString: String?)

    fun updateWarning(message: String)

    fun updateMaxAvailable(maxAmount: String)

    fun updateMaxAvailableColor(@ColorRes color: Int)

    fun updateReceivingAddress(address: String)

    fun updateFeeAmount(fee: String)

    // Set property
    fun setCryptoMaxLength(length: Int)

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun setFeePrioritySelection(index: Int)

    fun clearWarning()

    // Hide / Show
    fun hideReceivingDropdown()

    fun showReceivingDropdown()

    fun hideSendingFieldDropdown()

    fun showSendingFieldDropdown()

    fun showMaxAvailable()

    fun hideMaxAvailable()

    fun showFeePriority()

    fun hideFeePriority()

    // Enable / Disable
    fun disableCryptoTextChangeListener()

    fun enableCryptoTextChangeListener()

    fun disableFiatTextChangeListener()

    fun enableFiatTextChangeListener()

    fun enableFeeDropdown()

    fun disableFeeDropdown()

    fun setSendButtonEnabled(enabled: Boolean)

    fun disableInput()

    fun enableInput()

    // Fetch value
    fun getCustomFeeValue(): Long

    fun getClipboardContents(): String?

    fun getReceivingAddress(): String?

    fun getFeePriority(): Int

    // Prompts
    fun showSnackbar(@StringRes message: Int, duration: Int)

    fun showSnackbar(message: String, @Nullable extraInfo: String?, duration: Int)

    fun showEthContractSnackbar()

    fun showBIP38PassphrasePrompt(scanData: String)

    fun showWatchOnlyWarning(address: String)

    fun showProgressDialog(@StringRes title: Int)

    fun showSpendFromWatchOnlyWarning(address: String)

    fun showSecondPasswordDialog()

    fun showPaymentDetails(
        confirmationDetails: PaymentConfirmationDetails,
        note: String?,
        allowFeeChange: Boolean
    )

    fun showLargeTransactionWarning()

    fun showTransactionSuccess(
        hash: String,
        transactionValue: Long,
        cryptoCurrency: CryptoCurrency
    )

    fun dismissProgressDialog()

    fun dismissConfirmationDialog()

    fun finishPage()

    fun hideCurrencyHeader()
}
