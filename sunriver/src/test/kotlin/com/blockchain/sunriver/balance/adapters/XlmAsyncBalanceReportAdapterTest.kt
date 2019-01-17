package com.blockchain.sunriver.balance.adapters

import com.blockchain.balance.AsyncBalanceReporter
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.testutils.lumens
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class XlmAsyncBalanceReportAdapterTest {

    private val xlmDataManager: XlmDataManager = mock {
        on { getBalance() } `it returns` Single.just(100.lumens())
        on { getBalance(any<AccountReference.Xlm>()) } `it returns` Single.just(CryptoValue.ZeroXlm)
        on {
            getBalance(argThat<AccountReference.Xlm> { accountId == "GABC123" })
        } `it returns` Single.just(50.lumens())
    }
    private val balanceReporter: AsyncBalanceReporter = xlmDataManager.toAsyncBalanceReporter()

    @Test
    fun `value of entire balance comes from data manager wallet balance`() {
        balanceReporter.entireBalance()
            .test().values().single() `should equal` 100.lumens()
    }

    @Test
    fun `imported addresses just returns zero as no imported addresses on Xlm`() {
        balanceReporter.importedAddressBalance()
            .test().values().single() `should equal` CryptoValue.ZeroXlm
    }

    @Test
    fun `single address`() {
        balanceReporter.addressBalance("GABC123")
            .test().values().single() `should equal` 50.lumens()
    }

    @Test
    fun `single address of unknown address just returns zero`() {
        balanceReporter.addressBalance("GABC1234")
            .test().values().single() `should equal` CryptoValue.ZeroXlm
    }

    @Test
    fun `watch only just returns zero as no watch only addresses on Xlm`() {
        balanceReporter.watchOnlyBalance()
            .test().values().single() `should equal` CryptoValue.ZeroXlm
    }
}
