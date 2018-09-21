package com.blockchain.datamanagers

import com.blockchain.android.testutils.rxInit
import com.blockchain.datamanagers.fees.BitcoinLikeFees
import com.blockchain.datamanagers.fees.EthereumFees
import com.blockchain.datamanagers.fees.FeeType
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.apache.commons.lang3.tuple.Pair
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
        subject.executeTransaction(amount, destination, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        verify(sendDataManager).getSpendableCoins(
            unspentOutputs,
            amount.amount,
            bitcoinLikeNetworkFee.regularFeePerKb
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
        subject.executeTransaction(
            amount,
            destination,
            account,
            bitcoinLikeNetworkFee,
            FeeType.Priority
        )
            .test()
        // Assert
        verify(sendDataManager).getSpendableCoins(
            unspentOutputs,
            BigInteger.TEN,
            bitcoinLikeNetworkFee.priorityFeePerKb
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
                bitcoinLikeNetworkFee.regularFeePerKb,
                amount.amount
            )
        ).thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(sendDataManager).submitBtcPayment(
            spendable,
            listOf(ecKey),
            destination,
            change,
            bitcoinLikeNetworkFee.regularFeePerKb,
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
                bitcoinLikeNetworkFee.regularFeePerKb,
                amount.amount
            )
        ).thenReturn(Observable.just(txHash))
        // Act
        val testObserver =
            subject.executeTransaction(amount, destination, bchAccount, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(sendDataManager).submitBchPayment(
            spendable,
            listOf(ecKey),
            destination,
            change,
            bitcoinLikeNetworkFee.regularFeePerKb,
            amount.amount
        )
    }

    @Test
    fun `execute ethereum transaction verify entire flow`() {
        // Arrange
        val amount = CryptoValue.etherFromWei(10.toBigInteger())
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
                ethereumNetworkFee.gasPriceWei,
                ethereumNetworkFee.gasLimitWei,
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
        val testObserver =
            subject.executeTransaction(amount, destination, account, ethereumNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        verify(ethDataManager).createEthTransaction(
            BigInteger.ONE,
            destination,
            ethereumNetworkFee.gasPriceWei,
            ethereumNetworkFee.gasLimitWei,
            amount.amount
        )
    }

    @Test
    fun `get maximum spendable BTC with default regular fee`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.BTC
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getMaximumAvailable(
                unspentOutputs,
                bitcoinLikeNetworkFee.regularFeePerKb
            )
        ).thenReturn(Pair.of(BigInteger.TEN, BigInteger.TEN))
        // Act
        val testObserver =
            subject.getMaximumSpendable(cryptoCurrency, account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinFromSatoshis(10))
    }

    @Test
    fun `get maximum spendable BTC with priority fee`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.BTC
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getMaximumAvailable(
                unspentOutputs,
                bitcoinLikeNetworkFee.priorityFeePerKb
            )
        ).thenReturn(Pair.of(BigInteger.TEN, BigInteger.TEN))
        // Act
        val testObserver =
            subject.getMaximumSpendable(
                cryptoCurrency,
                account,
                bitcoinLikeNetworkFee,
                FeeType.Priority
            )
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinFromSatoshis(10))
    }

    @Test
    fun `get maximum spendable BTC should return zero on error`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.BTC
        val account = Account().apply { xpub = "XPUB" }
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver =
            subject.getMaximumSpendable(cryptoCurrency, account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroBtc)
    }

    @Test
    fun `get maximum spendable BCH`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.BCH
        val account = GenericMetadataAccount().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getMaximumAvailable(
                unspentOutputs,
                bitcoinLikeNetworkFee.regularFeePerKb
            )
        )
            .thenReturn(Pair.of(BigInteger.TEN, BigInteger.TEN))
        // Act
        val testObserver =
            subject.getMaximumSpendable(cryptoCurrency, account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinCashFromSatoshis(10))
    }

    @Test
    fun `get maximum spendable BCH should return zero on error`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.BCH
        val account = GenericMetadataAccount().apply { xpub = "XPUB" }
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver =
            subject.getMaximumSpendable(cryptoCurrency, account, bitcoinLikeNetworkFee)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroBch)
    }

    @Test
    fun `get maximum spendable ETH`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.ETHER
        val account = EthereumAccount()
        val combinedEthModel: CombinedEthModel = mock()
        val addressResponse: EthAddressResponse = mock()
        whenever(combinedEthModel.getAddressResponse()).thenReturn(addressResponse)
        whenever(addressResponse.balance).thenReturn(BigInteger.valueOf(1_000_000_000_000_000_000L))
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        // Act
        val testObserver = subject.getMaximumSpendable(cryptoCurrency, account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(
            CryptoValue.etherFromWei(
                1_000_000_000_000_000_000L.toBigInteger() -
                    ethereumNetworkFee.absoluteFee.amount
            )
        )
    }

    @Test
    fun `get maximum spendable ETH should not return less than zero`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.ETHER
        val account = EthereumAccount()
        val combinedEthModel: CombinedEthModel = mock()
        val addressResponse: EthAddressResponse = mock()
        whenever(combinedEthModel.getAddressResponse()).thenReturn(addressResponse)
        whenever(addressResponse.balance).thenReturn(BigInteger.ZERO)
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.just(combinedEthModel))
        // Act
        val testObserver = subject.getMaximumSpendable(cryptoCurrency, account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroEth)
    }

    @Test
    fun `get maximum spendable ETH should return zero if error fetching account`() {
        // Arrange
        val cryptoCurrency = CryptoCurrency.ETHER
        val account = EthereumAccount()
        whenever(ethDataManager.fetchEthAddress())
            .thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver = subject.getMaximumSpendable(cryptoCurrency, account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.ZeroEth)
    }

    @Test
    fun `get absolute fee for bitcoin`() {
        // Arrange
        val amount = 1.23.bitcoin()
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinFromSatoshis(500))
    }

    @Test
    fun `get absolute fee for bitcoin uses regular fee by default`() {
        // Arrange
        val amount = 1.23.bitcoin()
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getSpendableCoins(
                any(),
                any(),
                eq(bitcoinLikeNetworkFee.regularFeePerKb)
            )
        ).thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinFromSatoshis(500))
    }

    @Test
    fun `get absolute fee for bitcoin uses priority fee if specified`() {
        // Arrange
        val amount = 1.23.bitcoin()
        val account = Account().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(
            sendDataManager.getSpendableCoins(
                any(),
                any(),
                eq(bitcoinLikeNetworkFee.priorityFeePerKb)
            )
        ).thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver =
            subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee, FeeType.Priority)
                .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinFromSatoshis(500))
    }

    @Test
    fun `get absolute fee for bitcoin cash`() {
        // Arrange
        val amount = 1.23.bitcoinCash()
        val account = GenericMetadataAccount().apply { xpub = "XPUB" }
        val unspentOutputs = UnspentOutputs()
        whenever(sendDataManager.getUnspentBchOutputs("XPUB"))
            .thenReturn(Observable.just(unspentOutputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
            .thenReturn(SpendableUnspentOutputs().apply { absoluteFee = 500.toBigInteger() })
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, bitcoinLikeNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(CryptoValue.bitcoinCashFromSatoshis(500))
    }

    @Test
    fun `get absolute fee for ether`() {
        // Arrange
        val amount = 1.23.ether()
        val account = EthereumAccount()
        // Act
        val testObserver = subject.getFeeForTransaction(amount, account, ethereumNetworkFee)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(ethereumNetworkFee.absoluteFee)
    }

    @Test
    fun `get change address bitcoin`() {
        // Arrange
        val account = Account()
        whenever(payloadDataManager.getNextChangeAddress(account))
            .thenReturn(Observable.just("CHANGE"))
        // Act
        val testObserver = subject.getChangeAddress(CryptoCurrency.BTC, account)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("CHANGE")
    }

    @Test
    fun `get change address bitcoin cash`() {
        // Arrange
        val bchAccount = GenericMetadataAccount().apply { xpub = "XPUB" }
        val account = Account().apply { xpub = "XPUB" }
        whenever(bchDataManager.getActiveAccounts()).thenReturn(listOf(bchAccount))
        whenever(payloadDataManager.getAccountForXPub("XPUB"))
            .thenReturn(account)
        whenever(bchDataManager.getNextChangeAddress(0))
            .thenReturn(Observable.just("CHANGE"))
        // Act
        val testObserver = subject.getChangeAddress(CryptoCurrency.BCH, bchAccount)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("CHANGE")
    }

    @Test
    fun `get change address ethereum`() {
        // Arrange
        val account: EthereumAccount = mock()
        whenever(account.address).thenReturn("ADDRESS")
        // Act
        val testObserver = subject.getChangeAddress(CryptoCurrency.ETHER, account)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("ADDRESS")
    }

    @Test
    fun `get receive address bitcoin`() {
        // Arrange
        val account = Account()
        whenever(payloadDataManager.getNextReceiveAddress(account))
            .thenReturn(Observable.just("RECEIVE"))
        // Act
        val testObserver = subject.getReceiveAddress(CryptoCurrency.BTC, account)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("RECEIVE")
    }

    @Test
    fun `get receive address bitcoin cash`() {
        // Arrange
        val bchAccount = GenericMetadataAccount().apply { xpub = "XPUB" }
        val account = Account().apply { xpub = "XPUB" }
        whenever(bchDataManager.getActiveAccounts()).thenReturn(listOf(bchAccount))
        whenever(payloadDataManager.getAccountForXPub("XPUB"))
            .thenReturn(account)
        whenever(bchDataManager.getNextReceiveAddress(0))
            .thenReturn(Observable.just("RECEIVE"))
        // Act
        val testObserver = subject.getReceiveAddress(CryptoCurrency.BCH, bchAccount)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("RECEIVE")
    }

    @Test
    fun `get receive address ethereum`() {
        // Arrange
        val account: EthereumAccount = mock()
        whenever(account.address).thenReturn("ADDRESS")
        // Act
        val testObserver = subject.getReceiveAddress(CryptoCurrency.ETHER, account)
            .test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue("ADDRESS")
    }

    private val bitcoinLikeNetworkFee = BitcoinLikeFees(
        priorityFeePerByte = 100L,
        regularFeePerByte = 10L
    )

    private val ethereumNetworkFee = EthereumFees(
        gasPriceGwei = 10L,
        gasLimitGwei = 21000L
    )
}