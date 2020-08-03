package piuk.blockchain.android.ui.send

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.blockchain.transactions.Memo
import com.google.android.material.snackbar.Snackbar
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpView

interface SendView : MvpView {

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun updateReceivingHintAndAccountDropDowns(
        currency: CryptoCurrency,
        listSize: Int,
        pitAddressAvailable: Boolean,
        onPitClicked: () -> Unit = {}
    )

    // Update field
    fun updateSendingAddress(label: String)

    fun show2FANotAvailableError()

    fun fillOrClearAddress()

    fun updateCryptoAmount(cryptoValue: CryptoValue, silent: Boolean = false)

    fun updateFiatAmount(fiatValue: FiatValue, silent: Boolean = false)

    fun updateWarning(message: String)

    fun updateMaxAvailable(maxAmount: String)

    fun updateMaxAvailable(maxAmount: CryptoValue, min: CryptoValue)

    fun updateMaxAvailableColor(@ColorRes color: Int)

    fun updateReceivingAddress(address: String)

    fun updateFeeAmount(feeCrypto: CryptoValue, feeFiat: FiatValue)

    fun clearFeeAmount()

    fun clearAmount()

    fun lastEnteredCryptoAmount(): String

    // Set property
    fun setCryptoMaxLength(length: Int)

    fun setFeePrioritySelection(index: Int)

    fun clearWarning()

    // Hide / Show
    fun showMaxAvailable()

    fun hideMaxAvailable()

    fun showFeePriority()

    fun hideFeePriority()

    fun showMemo()

    fun hideMemo()

    fun displayMemo(usersMemo: Memo)

    fun enableMemo(enabled: Boolean)

    fun showInfoLink()

    fun hideInfoLink()

    // Enable / Disable
    fun enableFeeDropdown()

    fun disableFeeDropdown()

    fun onBitPayAddressScanned()

    fun setSendButtonEnabled(enabled: Boolean)

    fun disableInput()

    fun showBitPayTimerAndMerchantInfo(expiry: String, merchantName: String)

    fun resetBitpayState()

    fun enableInput()

    // Fetch value
    fun getCustomFeeValue(): Long

    fun getClipboardContents(): String?

    fun getReceivingAddress(): String?

    fun getFeePriority(): Int

    // Prompts
    fun showSnackbar(@StringRes message: Int, duration: Int)

    fun showSnackbar(message: String, extraInfo: String?, duration: Int)

    fun showEthContractSnackbar()

    fun showBIP38PassphrasePrompt(scanData: String)

    fun showWatchOnlyWarning(address: String)

    fun showSpendFromWatchOnlyWarning(address: String)

    fun showSecondPasswordDialog()

    fun showPaymentDetails(
        confirmationDetails: PaymentConfirmationDetails,
        note: String?,
        noteDescription: String?,
        allowFeeChange: Boolean
    )

    fun showPaymentDetails(confirmationDetails: SendConfirmationDetails)

    fun showLargeTransactionWarning()

    fun showTransactionSuccess(cryptoCurrency: CryptoCurrency)

    fun showTransactionFailed() = showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_LONG)

    fun showInsufficientGasDlg(cryptoCurrency: CryptoCurrency)

    fun dismissConfirmationDialog()

    fun finishPage()

    fun hideCurrencyHeader()

    fun hidePitIconForValidAddress()

    fun showPitIconIfAvailable()

    fun updateRequiredLabelVisibility(isVisible: Boolean)

    fun isPitEnabled(enabled: Boolean)
}
