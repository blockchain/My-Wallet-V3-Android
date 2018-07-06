package piuk.blockchain.android.ui.shapeshift.newexchange

import android.support.annotation.StringRes
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface NewExchangeView : View {

    val locale: Locale

    val shapeShiftApiKey: String

    val isBuyPermitted: Boolean

    fun updateUi(
        fromCurrency: CryptoCurrencies,
        toCurrency: CryptoCurrencies,
        fromLabel: String,
        toLabel: String,
        fiatHint: String
    )

    fun removeAllFocus()

    fun launchAccountChooserActivityTo()

    fun launchAccountChooserActivityFrom()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun finishPage()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateFromCryptoText(text: String)

    fun updateToCryptoText(text: String)

    fun updateFromFiatText(text: String)

    fun updateToFiatText(text: String)

    fun clearEditTexts()

    fun showAmountError(errorMessage: String)

    fun clearError()

    fun setButtonEnabled(enabled: Boolean)

    fun showQuoteInProgress(inProgress: Boolean)

    fun launchConfirmationPage(shapeShiftData: ShapeShiftData)

    fun showNoFunds(canBuy: Boolean)
}