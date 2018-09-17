package com.blockchain.datamanagers

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import java.math.BigInteger

class TransactionSendDataManagerTest {

    private lateinit var subject: TransactionSendDataManager
    private val payloadDataManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val ethereumAccountWrapper: EthereumAccountWrapper = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = TransactionSendDataManager(
            payloadDataManager,
            ethDataManager,
            bchDataManager,
            sendDataManager,
            ethereumAccountWrapper
        )
    }

    @Test
    fun `execute bitcoin transaction should set regular fee by default`() {
        // Arrange
        val amount = CryptoValue.bitcoinFromSatoshis(10)
        val destination = "DESTINATION"
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs())
        whenever(payloadDataManager.getNextChangeAddress(account))
            .thenReturn(Observable.just("CHANGE"))
        // Act
        subject.executeTransaction(amount, destination, account, feeOptions)
            .test()
        // Assert
        verify(sendDataManager).getUnspentOutputs("XPUB")
        verify(sendDataManager).getSpendableCoins(
            unspentOutputs,
            amount.amount,
            feeOptions.regularFee.toBigInteger()
        )
    }

    @Test
    fun `execute bitcoin transaction with high priority fee`() {
        // Arrange
        val amount = CryptoValue.bitcoinFromSatoshis(10)
        val destination = "DESTINATION"
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs())
        whenever(payloadDataManager.getNextChangeAddress(account))
            .thenReturn(Observable.just("CHANGE"))
        // Act
        subject.executeTransaction(amount, destination, account, feeOptions, FeeType.Priority)
            .test()
        // Assert
        verify(sendDataManager).getUnspentOutputs("XPUB")
        verify(sendDataManager).getSpendableCoins(
            unspentOutputs,
            BigInteger.TEN,
            feeOptions.priorityFee.toBigInteger()
        )
    }

    @Test
    fun `execute bitcoin transaction verify entire flow`() {
        // Arrange
        val amount = CryptoValue.bitcoinFromSatoshis(10)
        val destination = "DESTINATION"
        val change = "CHANGE"
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        val spendable = SpendableUnspentOutputs()
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
            .thenReturn(spendable)
        val ecKey = ECKey()
        whenever(payloadDataManager.getHDKeysForSigning(account, spendable))
            .thenReturn(listOf(ecKey))
        whenever(payloadDataManager.getNextChangeAddress(account))
            .thenReturn(Observable.just(change))
        val txHash = "TX_ HASH"
        whenever(
            sendDataManager.submitBtcPayment(
                spendable,
                listOf(ecKey),
                destination,
                change,
                feeOptions.regularFee.toBigInteger(),
                amount.amount
            )
        ).thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, account, feeOptions)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(sendDataManager).submitBtcPayment(
            spendable,
            listOf(ecKey),
            destination,
            change,
            feeOptions.regularFee.toBigInteger(),
            amount.amount
        )
    }

    @Test
    fun `execute bitcoin cash transaction verify entire flow`() {
        // Arrange
        val amount = CryptoValue.bitcoinCashFromSatoshis(10)
        val destination = "DESTINATION"
        val change = "CHANGE"
        val bchAccount = GenericMetadataAccount().apply { xpub = "XPUB" }
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        val spendable = SpendableUnspentOutputs()
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
            .thenReturn(spendable)
        whenever(bchDataManager.getActiveAccounts()).thenReturn(listOf(bchAccount))
        whenever(payloadDataManager.getAccountForXPub("XPUB"))
            .thenReturn(account)
        val ecKey = ECKey()
        whenever(payloadDataManager.getHDKeysForSigning(account, spendable))
            .thenReturn(listOf(ecKey))
        whenever(bchDataManager.getNextChangeAddress(0))
            .thenReturn(Observable.just(change))
        val txHash = "TX_ HASH"
        whenever(
            sendDataManager.submitBchPayment(
                spendable,
                listOf(ecKey),
                destination,
                change,
                feeOptions.regularFee.toBigInteger(),
                amount.amount
            )
        ).thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, bchAccount, feeOptions)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(sendDataManager).submitBchPayment(
            spendable,
            listOf(ecKey),
            destination,
            change,
            feeOptions.regularFee.toBigInteger(),
            amount.amount
        )
    }

    @Test
    fun `execute ethereum transaction verify entire flow`() {
        // Arrange
        val amount = CryptoValue.etherFromWei(10)
        val destination = "DESTINATION"
        val account: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(combinedEthModel)
        whenever(combinedEthModel.getNonce())
            .thenReturn(BigInteger.ONE)
        val rawTransaction: RawTransaction = mock()
        whenever(
            ethDataManager.createEthTransaction(
                BigInteger.ONE,
                destination,
                feeOptions.regularFee.gasPriceToWei(),
                feeOptions.gasLimit.toBigInteger(),
                amount.amount
            )
        ).thenReturn(rawTransaction)
        val deterministicKey: DeterministicKey = mock()
        whenever(payloadDataManager.masterKey)
            .thenReturn(deterministicKey)
        val ecKey = ECKey()
        whenever(ethereumAccountWrapper.deriveECKey(deterministicKey, 0))
            .thenReturn(ecKey)
        val signedTx = ByteArray(0)
        whenever(account.signTransaction(rawTransaction, ecKey))
            .thenReturn(signedTx)
        val txHash = "TX_HASH"
        whenever(ethDataManager.pushEthTx(signedTx))
            .thenReturn(Observable.just(txHash))
        whenever(ethDataManager.setLastTxHashObservable(eq(txHash), any()))
            .thenReturn(Observable.just(txHash))
        // Act
        val testObserver = subject.executeTransaction(amount, destination, account, feeOptions)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(ethDataManager).createEthTransaction(
            BigInteger.ONE,
            destination,
            feeOptions.regularFee.gasPriceToWei(),
            feeOptions.gasLimit.toBigInteger(),
            amount.amount
        )
    }

    private val feeOptions = FeeOptions().apply {
        priorityFee = 100L
        regularFee = 10L
        gasLimit = 21000L
    }
}