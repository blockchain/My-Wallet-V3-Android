package com.blockchain.morph.ui.homebrew.exchange.confirmation

import com.blockchain.android.testutils.rxInit
import com.blockchain.datamanagers.TransactionSendDataManager
import com.blockchain.datamanagers.fees.BitcoinLikeFees
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.Account
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

class ExchangeConfirmationPresenterTest {

    private lateinit var subject: ExchangeConfirmationPresenter
    private val transactionSendDataManager: TransactionSendDataManager = mock()
    private val tradeExecutionService: TradeExecutionService = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val view: ExchangeConfirmationView = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = ExchangeConfirmationPresenter(
            transactionSendDataManager,
            tradeExecutionService,
            feeDataManager,
            payloadDataManager,
            bchDataManager,
            ethDataManager,
            environmentConfig
        )
        subject.initView(view)

        whenever(view.locale).thenReturn(Locale.US)
    }

    @Test
    fun `on view ready double encrypted`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        whenever(view.clickEvents).thenReturn(Observable.empty())
        // Act
        subject.onViewReady()
        // Assert
        verify(view).showSecondPasswordDialog()
    }

    @Test
    fun `update fee success`() {
        // Arrange
        val accountReference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "Label", "xPub")
        val sendingAccount = Account()
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.getAccountForXPub("xPub")).thenReturn(sendingAccount)
        val bitcoinLikeFees = BitcoinLikeFees(10, 100)
        val feeOptions = FeeOptions().apply {
            regularFee = 10L
            priorityFee = 100L
        }
        whenever(feeDataManager.btcFeeOptions).thenReturn(Observable.just(feeOptions))
        val fee = CryptoValue.bitcoinFromMajor(0.00005.toBigDecimal())
        whenever(
            transactionSendDataManager.getFeeForTransaction(
                1.0.bitcoin(),
                sendingAccount,
                bitcoinLikeFees
            )
        ).thenReturn(Single.just(fee))
        // Act
        subject.updateFee(1.0.bitcoin(), accountReference)
        // Assert
        verify(view).updateFee(fee)
    }

    @Test
    fun `update fee failure`() {
        // Arrange
        val accountReference = AccountReference.BitcoinLike(CryptoCurrency.BTC, "Label", "xPub")
        val sendingAccount = Account()
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.getAccountForXPub("xPub")).thenReturn(sendingAccount)
        val bitcoinLikeFees = BitcoinLikeFees(10, 100)
        val feeOptions = FeeOptions().apply {
            regularFee = 10L
            priorityFee = 100L
        }
        whenever(feeDataManager.btcFeeOptions).thenReturn(Observable.just(feeOptions))
        whenever(
            transactionSendDataManager.getFeeForTransaction(
                1.0.bitcoin(),
                sendingAccount,
                bitcoinLikeFees
            )
        ).thenReturn(Single.error { Throwable() })
        // Act
        subject.updateFee(1.0.bitcoin(), accountReference)
        // Assert
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
    }
}