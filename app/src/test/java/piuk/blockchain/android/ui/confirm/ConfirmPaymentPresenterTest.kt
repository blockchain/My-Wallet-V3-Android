package piuk.blockchain.android.ui.confirm

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.androidcoreui.ui.base.UiState

class ConfirmPaymentPresenterTest {

    private lateinit var subject: ConfirmPaymentPresenter
    private val mockActivity: ConfirmPaymentView = mock()

    @Before
    fun setUp() {
        subject = ConfirmPaymentPresenter()
        subject.initView(mockActivity)
    }

    @Test
    fun `onViewReady payment details null`() {
        // Arrange

        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).closeDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewReady() {
        // Arrange
        val fromLabel = "FROM_LABEL"
        val toLabel = "TO_LABEL"
        val btcAmount = "BTC_AMOUNT"
        val btcUnit = "BTC_UNIT"
        val fiatAmount = "FIAT_AMOUNT"
        val fiatSymbol = "FIAT_SYMBOL"
        val btcFee = "BTC_FEE"
        val fiatFee = "FIAT_FEE"
        val btcTotal = "BTC_TOTAL"
        val fiatTotal = "FIAT_TOTAL"
        val confirmationDetails = PaymentConfirmationDetails().apply {
            this.fromLabel = fromLabel
            this.toLabel = toLabel
            this.cryptoAmount = btcAmount
            this.cryptoUnit = btcUnit
            this.fiatAmount = fiatAmount
            this.fiatSymbol = fiatSymbol
            this.cryptoFee = btcFee
            this.fiatFee = fiatFee
            this.cryptoTotal = btcTotal
            this.fiatTotal = fiatTotal
        }
        val contactNote = "CONTACT_NOTE"
        whenever(mockActivity.paymentDetails).thenReturn(confirmationDetails)
        whenever(mockActivity.contactNote).thenReturn(contactNote)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).contactNote
        verify(mockActivity).setFromLabel(fromLabel)
        verify(mockActivity).setToLabel(toLabel)
        verify(mockActivity).setAmount("$btcAmount $btcUnit ($fiatSymbol$fiatAmount)")
        verify(mockActivity).setFee("$btcFee $btcUnit ($fiatSymbol$fiatFee)")
        verify(mockActivity).setTotalBtc("$btcTotal $btcUnit")
        verify(mockActivity).setTotalFiat("$fiatSymbol$fiatTotal")
        verify(mockActivity).contactNote = contactNote
        verify(mockActivity).setUiState(UiState.CONTENT)
        verifyNoMoreInteractions(mockActivity)
    }
}