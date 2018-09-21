package com.blockchain.datamanagers.fees

import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.fees.FeeDataManager

class FeeDataManagerExtensionsKtTest {

    private val feeDataManager: FeeDataManager = mock()

    @Before
    fun setUp() {
        whenever(feeDataManager.bchFeeOptions).thenReturn(Observable.just(feeOptions))
        whenever(feeDataManager.btcFeeOptions).thenReturn(Observable.just(feeOptions))
        whenever(feeDataManager.ethFeeOptions).thenReturn(Observable.just(feeOptions))
    }

    @Test
    fun `given btc, return correct bitcoin-like fees`() {
        whenever(feeDataManager.btcFeeOptions).thenReturn(Observable.just(feeOptions))
        feeDataManager.getFeeOptions(CryptoCurrency.BTC)
            .test()
            .values()
            .single()
            .apply {
                this `should equal` BitcoinLikeFees(feeOptions.regularFee, feeOptions.priorityFee)
            }
    }

    @Test
    fun `given bch, return correct bitcoin-like fees`() {
        whenever(feeDataManager.bchFeeOptions).thenReturn(Observable.just(feeOptions))
        feeDataManager.getFeeOptions(CryptoCurrency.BCH)
            .test()
            .values()
            .single()
            .apply {
                this `should equal` BitcoinLikeFees(feeOptions.regularFee, feeOptions.priorityFee)
            }
    }

    @Test
    fun `given eth, return correct ethereum fees`() {
        whenever(feeDataManager.ethFeeOptions).thenReturn(Observable.just(feeOptions))
        feeDataManager.getFeeOptions(CryptoCurrency.ETHER)
            .test()
            .values()
            .single()
            .apply {
                this `should equal` EthereumFees(feeOptions.regularFee, feeOptions.gasLimit)
            }
    }

    private val feeOptions = FeeOptions().apply {
        priorityFee = 100L
        regularFee = 10L
        gasLimit = 21000L
    }
}