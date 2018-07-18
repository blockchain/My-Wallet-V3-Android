package piuk.blockchain.androidcore.data.transactions.models

import info.blockchain.wallet.multiaddress.TransactionSummary
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal to`
import org.amshove.kluent.`should not equal`
import org.junit.Test
import java.math.BigInteger

class DisplayableTest {

    @Test
    fun `ensure not equal when compared to different type`() {
        val transactionSummary = TransactionSummary().apply {
            direction = TransactionSummary.Direction.TRANSFERRED
            time = 0L
            total = BigInteger.TEN
            fee = BigInteger.ONE
            hash = "HASH"
            inputsMap = HashMap<String, BigInteger>()
            outputsMap = HashMap<String, BigInteger>()
            confirmations = 10
            isWatchOnly = false
            isDoubleSpend = false
            isPending = false
        }

        val displayable1 = BtcDisplayable(transactionSummary).apply { note = "note 1" }
        val objectToCompare = Any()

        displayable1.toString() `should not equal to` objectToCompare.toString()
        displayable1.hashCode() `should not equal to` objectToCompare.hashCode()
        displayable1 `should not equal` objectToCompare
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with subtly different objects`() {
        val transactionSummary = TransactionSummary().apply {
            direction = TransactionSummary.Direction.TRANSFERRED
            time = 0L
            total = BigInteger.TEN
            fee = BigInteger.ONE
            hash = "HASH"
            inputsMap = HashMap<String, BigInteger>()
            outputsMap = HashMap<String, BigInteger>()
            confirmations = 10
            isWatchOnly = false
            isDoubleSpend = false
            isPending = false
        }

        val displayable1 = BtcDisplayable(transactionSummary).apply { note = "note 1" }
        val displayable2 = BtcDisplayable(transactionSummary).apply { note = "note 2" }

        displayable1.toString() `should not equal to` displayable2.toString()
        displayable1.hashCode() `should not equal to` displayable2.hashCode()
        displayable1 `should not equal` displayable2
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with identical objects`() {
        val transactionSummary = TransactionSummary().apply {
            direction = TransactionSummary.Direction.TRANSFERRED
            time = 0L
            total = BigInteger.TEN
            fee = BigInteger.ONE
            hash = "HASH"
            inputsMap = HashMap<String, BigInteger>()
            outputsMap = HashMap<String, BigInteger>()
            confirmations = 10
            isWatchOnly = false
            isDoubleSpend = false
            isPending = false
        }

        val displayable1 = BtcDisplayable(transactionSummary).apply { note = "note" }
        val displayable2 = BtcDisplayable(transactionSummary).apply { note = "note" }

        displayable1.toString() `should equal to` displayable2.toString()
        displayable1.hashCode() `should equal to` displayable2.hashCode()
        displayable1 `should equal` displayable2
    }
}