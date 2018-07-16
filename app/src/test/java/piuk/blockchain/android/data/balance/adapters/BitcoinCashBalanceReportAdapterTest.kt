package piuk.blockchain.android.data.balance.adapters

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.BalanceReporter
import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import java.math.BigInteger

class BitcoinCashBalanceReportAdapterTest {

    val dataManager: BchDataManager = mock {
        on { it.getWalletBalance() } `it returns` BigInteger.ZERO
        on { it.getImportedAddressBalance() } `it returns` BigInteger.ZERO
        on { it.getAddressBalance(any()) } `it returns` BigInteger.ZERO
    }
    val balanceReporter: BalanceReporter = dataManager.toBalanceReporter()

    @Test
    fun `value of entire balance comes from data manager wallet balance`() {
        whenever(dataManager.getWalletBalance()).thenReturn(BigInteger.valueOf(1234L))
        balanceReporter.entireBalance() `should equal` CryptoValue.bitcoinCashFromSatoshis(1234L)
    }

    @Test
    fun `value of imported address balance comes from data manager imported address balance`() {
        whenever(dataManager.getImportedAddressBalance()).thenReturn(BigInteger.valueOf(4567L))
        balanceReporter.importedAddressBalance() `should equal` CryptoValue.bitcoinCashFromSatoshis(4567L)
    }

    @Test
    fun `value of single address comes from data manager address balance`() {
        whenever(dataManager.getAddressBalance("mpxqy2yDLebDHuUVugcHrbS729HtxzRZtM")).thenReturn(
            BigInteger.valueOf(
                8901L
            )
        )
        balanceReporter.addressBalance("mpxqy2yDLebDHuUVugcHrbS729HtxzRZtM") `should equal`
            CryptoValue.bitcoinCashFromSatoshis(8901L)
    }
}
