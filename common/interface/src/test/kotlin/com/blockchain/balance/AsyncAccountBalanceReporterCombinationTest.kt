package com.blockchain.balance

import com.blockchain.testutils.bitcoin
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`it throws`
import org.junit.Test

class AsyncAccountBalanceReporterCombinationTest {

    @Test
    fun `when results are empty, returns empty`() {
        val reporter = empty() + empty()
        reporter.balanceOf(AccountReference.Ethereum("", ""))
            .test().assertComplete().assertValueCount(0)
    }

    @Test
    fun `when first result is non-empty, returns first result`() {
        val reporter = just(1.bitcoin()) + notExpected()
        reporter.balanceOf(AccountReference.Ethereum("", ""))
            .test().assertComplete().assertValueCount(1).assertValue(1.bitcoin())
    }

    @Test
    fun `when first result is empty, returns second result`() {
        val reporter = empty() + just(2.bitcoin())
        reporter.balanceOf(AccountReference.Ethereum("", ""))
            .test().assertComplete().assertValueCount(1).assertValue(2.bitcoin())
    }

    private fun empty() =
        mock<AsyncAccountBalanceReporter> {
            on { balanceOf(any()) } `it returns` Maybe.empty()
        }

    private fun notExpected() =
        mock<AsyncAccountBalanceReporter> {
            on { balanceOf(any()) } `it throws` RuntimeException("X")
        }

    private fun just(cryptoValue: CryptoValue) =
        mock<AsyncAccountBalanceReporter> {
            on { balanceOf(any()) } `it returns` Maybe.just(cryptoValue)
        }
}
