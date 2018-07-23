package piuk.blockchain.android.ui.balance

import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.balance.adapter.formatting
import piuk.blockchain.androidcore.data.transactions.models.Displayable

class DisplayableFormattingTest {

    @Test
    fun `sent text`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.SENT
            on { cryptoCurrency } `it returns` CryptoCurrency.BTC
            on { confirmations } `it returns` CryptoCurrency.BTC.requiredConfirmations
        }.formatting().apply {
            text `should equal` R.string.SENT
        }
    }

    @Test
    fun `received text`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.SENT
            on { cryptoCurrency } `it returns` CryptoCurrency.BCH
            on { confirmations } `it returns` CryptoCurrency.BCH.requiredConfirmations
        }.formatting().apply {
            text `should equal` R.string.SENT
        }
    }

    @Test
    fun `transferred text`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.TRANSFERRED
            on { cryptoCurrency } `it returns` CryptoCurrency.ETHER
            on { confirmations } `it returns` CryptoCurrency.ETHER.requiredConfirmations
        }.formatting().apply {
            text `should equal` R.string.MOVED
        }
    }

    @Test
    fun `after send confirmed, should be red sent colour`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.SENT
            on { cryptoCurrency } `it returns` CryptoCurrency.BTC
            on { confirmations } `it returns` CryptoCurrency.BTC.requiredConfirmations
        }.formatting().apply {
            directionColour `should equal` R.color.product_red_sent
            valueBackground `should equal` R.drawable.rounded_view_red
        }
    }

    @Test
    fun `before send confirmed, should be half red sent colour`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.SENT
            on { cryptoCurrency } `it returns` CryptoCurrency.BTC
            on { confirmations } `it returns` CryptoCurrency.BTC.requiredConfirmations - 1
        }.formatting().apply {
            directionColour `should equal` R.color.product_red_sent_50
            valueBackground `should equal` R.drawable.rounded_view_red_50
        }
    }

    @Test
    fun `after received confirmed, should be green received colour`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.RECEIVED
            on { cryptoCurrency } `it returns` CryptoCurrency.ETHER
            on { confirmations } `it returns` CryptoCurrency.ETHER.requiredConfirmations
        }.formatting().apply {
            directionColour `should equal` R.color.product_green_received
            valueBackground `should equal` R.drawable.rounded_view_green
        }
    }

    @Test
    fun `before received confirmed, should be half green received colour`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.RECEIVED
            on { cryptoCurrency } `it returns` CryptoCurrency.ETHER
            on { confirmations } `it returns` CryptoCurrency.ETHER.requiredConfirmations - 1
        }.formatting().apply {
            directionColour `should equal` R.color.product_green_received_50
            valueBackground `should equal` R.drawable.rounded_view_green_50
        }
    }

    @Test
    fun `after transfer confirmed, should be gray transferred colour`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.TRANSFERRED
            on { cryptoCurrency } `it returns` CryptoCurrency.BCH
            on { confirmations } `it returns` CryptoCurrency.BCH.requiredConfirmations
        }.formatting().apply {
            directionColour `should equal` R.color.product_gray_transferred
            valueBackground `should equal` R.drawable.rounded_view_transferred
        }
    }

    @Test
    fun `before transfer confirmed, should be half gray transferred colour`() {
        mock<Displayable> {
            on { direction } `it returns` TransactionSummary.Direction.TRANSFERRED
            on { cryptoCurrency } `it returns` CryptoCurrency.BCH
            on { confirmations } `it returns` CryptoCurrency.BCH.requiredConfirmations - 1
        }.formatting().apply {
            directionColour `should equal` R.color.product_gray_transferred_50
            valueBackground `should equal` R.drawable.rounded_view_transferred_50
        }
    }
}