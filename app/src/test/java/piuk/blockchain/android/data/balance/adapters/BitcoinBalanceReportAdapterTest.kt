package piuk.blockchain.android.data.balance.adapters

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.BalanceReporter
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.PayloadManager
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigInteger

class BitcoinBalanceReportAdapterTest {

    val payloadManager: PayloadManager = mock {
        on { it.walletBalance } `it returns` BigInteger.ZERO
        on { it.importedAddressesBalance } `it returns` BigInteger.ZERO
        on { it.getAddressBalance(any()) } `it returns` BigInteger.ZERO
    }
    val balanceReporter: BalanceReporter = payloadManager.toBalanceReporter()

    @Test
    fun `value of entire balance comes from payload manager wallet balance`() {
        whenever(payloadManager.walletBalance).thenReturn(BigInteger.valueOf(1234L))
        balanceReporter.entireBalance() `should equal` CryptoValue.bitcoinFromSatoshis(1234L)
    }

    @Test
    fun `value of imported address balance comes from payload manager imported address balance`() {
        whenever(payloadManager.importedAddressesBalance).thenReturn(BigInteger.valueOf(4567L))
        balanceReporter.importedAddressBalance() `should equal` CryptoValue.bitcoinFromSatoshis(4567L)
    }

    @Test
    fun `value of single address comes from payload manager address balance`() {
        whenever(payloadManager.getAddressBalance("mjoGVXRDjxqA2oY23Qm1kaLTQoNUmxd6jq")).thenReturn(
            BigInteger.valueOf(
                8901L
            )
        )
        balanceReporter.addressBalance("mjoGVXRDjxqA2oY23Qm1kaLTQoNUmxd6jq") `should equal`
            CryptoValue.bitcoinFromSatoshis(8901L)
    }
}
