package piuk.blockchain.android.ui.shapeshift.confirmation

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.apache.commons.lang3.NotImplementedException
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import piuk.blockchain.android.util.StringUtils
import retrofit2.Retrofit
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit

class ShapeShiftConfirmationPresenterTest : RxTest() {

    private lateinit var subject: ShapeShiftConfirmationPresenter
    private val view: ShapeShiftConfirmationView = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val payloadDataManager: PayloadDataManager =
            mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val sendDataManager: SendDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val ethereumAccountWrapper: EthereumAccountWrapper = mock()

    private val orderId = "ORDER_ID"
    private val changeAddress = "CHANGE_ADDRESS"
    private val withdrawalAddress = "WITHDRAWAL_ADDRESS"
    private val returnAddress = "RETURN_ADDRESS"
    private val xPub = "X_PUB"

    private val fromBtc = ShapeShiftData(
            orderId = orderId,
            fromCurrency = CryptoCurrencies.BTC,
            toCurrency = CryptoCurrencies.ETHER,
            depositAmount = BigDecimal.valueOf(1.0),
            depositAddress = "17MgvXUa6tPsh3KMRWAPYBuDwbtCBF6Py5",
            changeAddress = changeAddress,
            withdrawalAmount = BigDecimal.valueOf(1.0),
            withdrawalAddress = withdrawalAddress,
            exchangeRate = BigDecimal.TEN,
            transactionFee = BigInteger.TEN,
            networkFee = BigDecimal.TEN,
            returnAddress = returnAddress,
            xPub = xPub,
            expiration = 0L,
            gasLimit = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            feePerKb = BigInteger.ONE
    )

    private val fromEth = ShapeShiftData(
            orderId = orderId,
            fromCurrency = CryptoCurrencies.ETHER,
            toCurrency = CryptoCurrencies.BCH,
            depositAmount = BigDecimal.valueOf(1.0),
            depositAddress = "0xF85608F8fe3887Dab333Ec250A972C1DC19C52B3",
            changeAddress = changeAddress,
            withdrawalAmount = BigDecimal.valueOf(1.0),
            withdrawalAddress = withdrawalAddress,
            exchangeRate = BigDecimal.TEN,
            transactionFee = BigInteger.valueOf(1_000_000_000_000_000),
            networkFee = BigDecimal.TEN,
            returnAddress = returnAddress,
            xPub = xPub,
            expiration = System.currentTimeMillis() + 5000L,
            gasLimit = BigInteger.TEN,
            gasPrice = BigInteger.TEN,
            feePerKb = BigInteger.ONE
    )

    private val fromBch = ShapeShiftData(
            orderId = orderId,
            fromCurrency = CryptoCurrencies.BCH,
            toCurrency = CryptoCurrencies.ETHER,
            depositAmount = BigDecimal.valueOf(1.0),
            depositAddress = "17MgvXUa6tPsh3KMRWAPYBuDwbtCBF6Py5",
            changeAddress = changeAddress,
            withdrawalAmount = BigDecimal.valueOf(1.0),
            withdrawalAddress = withdrawalAddress,
            exchangeRate = BigDecimal.TEN,
            transactionFee = BigInteger.TEN,
            networkFee = BigDecimal.TEN,
            returnAddress = returnAddress,
            xPub = xPub,
            expiration = 0L,
            gasLimit = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            feePerKb = BigInteger.ONE
    )

    @Before
    override fun setUp() {
        super.setUp()

        subject = ShapeShiftConfirmationPresenter(
                shapeShiftDataManager,
                payloadDataManager,
                sendDataManager,
                ethDataManager,
                bchDataManager,
                stringUtils,
                ethereumAccountWrapper
        ).apply { initView(this@ShapeShiftConfirmationPresenterTest.view) }

        whenever(view.locale).thenReturn(Locale.US)

        initFramework()
    }

    @Test
    fun `onViewReady from BTC should render UI and show quote expired`() {
        // Arrange
        whenever(view.shapeShiftData).thenReturn(fromBtc)
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_deposit_title,
                        fromBtc.fromCurrency.unit
                )
        ).thenReturn("${fromBtc.fromCurrency.unit} to deposit")
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_receive_title,
                        fromBtc.toCurrency.unit
                )
        ).thenReturn("${fromBtc.toCurrency.unit} to receive")
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_total_title,
                        fromBtc.fromCurrency.unit
                )
        ).thenReturn("Total ${fromBtc.fromCurrency.unit} spent")
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_exchange_rate_formatted,
                        fromBtc.fromCurrency.symbol,
                        subject.decimalFormat.format(
                                fromBtc.exchangeRate.setScale(
                                        8,
                                        RoundingMode.HALF_DOWN
                                )
                        ),
                        fromBtc.toCurrency.symbol
                )
        ).thenReturn("1 ${fromBtc.fromCurrency.symbol} = ${fromBtc.exchangeRate.toPlainString()} ${fromBtc.toCurrency.symbol}")
        // Act
        subject.onViewReady()
        // Assert
        verify(view).updateDeposit(
                "${fromBtc.fromCurrency.unit} to deposit",
                "1.0 ${fromBtc.fromCurrency.symbol}"
        )
        verify(view).updateReceive(
                "${fromBtc.toCurrency.unit} to receive",
                "1.0 ${fromBtc.toCurrency.symbol}"
        )
        verify(view).updateTotalAmount(
                "Total ${fromBtc.fromCurrency.unit} spent",
                "1.0000001 ${fromBtc.fromCurrency.symbol}"
        )
        verify(view).updateExchangeRate("1 ${fromBtc.fromCurrency.symbol} = ${fromBtc.exchangeRate.toPlainString()} ${fromBtc.toCurrency.symbol}")
        verify(view).updateTransactionFee("0.0000001 ${fromBtc.fromCurrency.symbol}")
        verify(view).updateNetworkFee("10.0 ${fromBtc.toCurrency.symbol}")
        verify(view).showQuoteExpiredDialog()
    }

    @Test
    fun `onViewReady from ETH should render UI and count down before timing out`() {
        // Arrange
        whenever(view.shapeShiftData).thenReturn(fromEth)
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_deposit_title,
                        fromEth.fromCurrency.unit
                )
        ).thenReturn("${fromEth.fromCurrency.unit} to deposit")
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_receive_title,
                        fromEth.toCurrency.unit
                )
        ).thenReturn("${fromEth.toCurrency.unit} to receive")
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_total_title,
                        fromEth.fromCurrency.unit
                )
        ).thenReturn("Total ${fromEth.fromCurrency.unit} spent")
        whenever(
                stringUtils.getFormattedString(
                        R.string.shapeshift_exchange_rate_formatted,
                        fromEth.fromCurrency.symbol,
                        subject.decimalFormat.format(
                                fromEth.exchangeRate.setScale(
                                        8,
                                        RoundingMode.HALF_DOWN
                                )
                        ),
                        fromEth.toCurrency.symbol
                )
        ).thenReturn("1 ${fromEth.fromCurrency.symbol} = ${fromEth.exchangeRate.toPlainString()} ${fromEth.toCurrency.symbol}")
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(view).updateDeposit(
                "${fromEth.fromCurrency.unit} to deposit",
                "1.0 ${fromEth.fromCurrency.symbol}"
        )
        verify(view).updateReceive(
                "${fromEth.toCurrency.unit} to receive",
                "1.0 ${fromEth.toCurrency.symbol}"
        )
        verify(view).updateTotalAmount(
                "Total ${fromEth.fromCurrency.unit} spent",
                "1.001 ${fromEth.fromCurrency.symbol}"
        )
        verify(view).updateExchangeRate("1 ${fromEth.fromCurrency.symbol} = ${fromEth.exchangeRate.toPlainString()} ${fromEth.toCurrency.symbol}")
        verify(view).updateTransactionFee("0.001 ${fromEth.fromCurrency.symbol}")
        verify(view).updateNetworkFee("10.0 ${fromEth.toCurrency.symbol}")
        verify(view, atLeastOnce()).updateCounter(any())
        verify(view).showQuoteExpiredDialog()
    }

    @Test
    fun onAcceptTermsClicked() {
        // Arrange

        // Act
        subject.onAcceptTermsClicked()
        // Assert
        verify(view).setButtonState(true)
    }

    @Test
    fun `onConfirmClicked terms not accepted`() {
        // Arrange

        // Act
        subject.onConfirmClicked()
        // Assert
        verifyZeroInteractions(payloadDataManager)
        verifyZeroInteractions(bchDataManager)
        verifyZeroInteractions(ethDataManager)
    }

    @Test
    fun `onConfirmClicked terms accepted but second password enabled`() {
        // Arrange
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(true)
        // Act
        subject.onAcceptTermsClicked()
        subject.onConfirmClicked()
        // Assert
        verify(payloadDataManager).isDoubleEncrypted
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showSecondPasswordDialog()
    }

    @Test
    fun `onConfirmClicked from BTC failure`() {
        // Arrange
        val account: Account = mock()
        whenever(view.shapeShiftData).thenReturn(fromBtc)
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.getAccountForXPub(fromBtc.xPub)).thenReturn(account)
        whenever(payloadDataManager.getAddressBalance(fromBtc.xPub)).thenReturn(BigInteger.ONE)
        whenever(sendDataManager.getUnspentOutputs(fromBtc.xPub))
                .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onAcceptTermsClicked()
        subject.onConfirmClicked()
        // Assert
        verify(payloadDataManager, times(2)).isDoubleEncrypted
        verify(payloadDataManager).getAccountForXPub(fromBtc.xPub)
        verify(payloadDataManager).getAddressBalance(fromBtc.xPub)
        verifyNoMoreInteractions(payloadDataManager)
        verify(sendDataManager).getUnspentOutputs(fromBtc.xPub)
        verifyNoMoreInteractions(sendDataManager)
        verify(view).setButtonState(true)
        verify(view, atLeastOnce()).shapeShiftData
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onConfirmClicked from BTC`() {
        // Arrange
        val account: Account = mock()
        val outputs: UnspentOutputs = mock()
        val spendableUnspentOutputs: SpendableUnspentOutputs = mock()
        val ecKey: ECKey = mock()
        val txHash = "TX_HASH"
        whenever(view.shapeShiftData).thenReturn(fromBtc)
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.getAccountForXPub(fromBtc.xPub)).thenReturn(account)
        whenever(payloadDataManager.getAddressBalance(fromBtc.xPub)).thenReturn(BigInteger.ONE)
        whenever(sendDataManager.getUnspentOutputs(fromBtc.xPub))
                .thenReturn(Observable.just(outputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
                .thenReturn(spendableUnspentOutputs)
        whenever(payloadDataManager.getHDKeysForSigning(account, spendableUnspentOutputs))
                .thenReturn(listOf(ecKey))
        whenever(
                sendDataManager.submitBtcPayment(
                        spendableUnspentOutputs,
                        listOf(ecKey),
                        fromBtc.depositAddress,
                        fromBtc.changeAddress,
                        fromBtc.transactionFee,
                        fromBtc.depositAmount.multiply(BigDecimal.valueOf(1e8)).toBigInteger()
                )
        ).thenReturn(Observable.just(txHash))
        whenever(shapeShiftDataManager.addTradeToList(any())).thenReturn(Completable.complete())
        // Act
        subject.onAcceptTermsClicked()
        subject.onConfirmClicked()
        // Assert
        verify(payloadDataManager, times(2)).isDoubleEncrypted
        verify(payloadDataManager).getAccountForXPub(fromBtc.xPub)
        verify(payloadDataManager).getAddressBalance(fromBtc.xPub)
        verify(payloadDataManager).getHDKeysForSigning(account, spendableUnspentOutputs)
        verifyNoMoreInteractions(payloadDataManager)
        verify(sendDataManager).getUnspentOutputs(fromBtc.xPub)
        verify(sendDataManager).getSpendableCoins(any(), any(), any())
        verify(sendDataManager).submitBtcPayment(
                spendableUnspentOutputs,
                listOf(ecKey),
                fromBtc.depositAddress,
                fromBtc.changeAddress,
                fromBtc.transactionFee,
                fromBtc.depositAmount.multiply(BigDecimal.valueOf(1e8)).toBigInteger()
        )
        verifyNoMoreInteractions(sendDataManager)
        verify(shapeShiftDataManager).addTradeToList(any())
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).setButtonState(true)
        verify(view, atLeastOnce()).shapeShiftData
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).launchProgressPage(fromBtc.depositAddress)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onConfirmClicked from ETH`() {
        // Arrange
        val combinedEthModel: CombinedEthModel = mock()
        val nonce = BigInteger.ONE
        whenever(combinedEthModel.getNonce()).thenReturn(nonce)
        val rawTransaction: RawTransaction = mock()
        val txHash = "TX_HASH"
        val signedTx = byteArrayOf()
        val deterministicKey: DeterministicKey = mock()
        val ecKey: ECKey = mock()
        whenever(view.shapeShiftData).thenReturn(fromEth)
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.wallet!!.hdWallets[0].masterKey).thenReturn(deterministicKey)
        whenever(ethereumAccountWrapper.deriveECKey(deterministicKey, 0))
                .thenReturn(ecKey)
        whenever(ethDataManager.fetchEthAddress())
                .thenReturn(Observable.just(mock(CombinedEthModel::class)))
        whenever(ethDataManager.getEthResponseModel()).thenReturn(combinedEthModel)
        whenever(
                ethDataManager.createEthTransaction(
                        nonce,
                        fromEth.depositAddress,
                        Convert.toWei(BigDecimal(fromEth.gasPrice), Convert.Unit.GWEI).toBigInteger(),
                        fromEth.gasLimit,
                        Convert.toWei(
                                fromEth.depositAmount,
                                Convert.Unit.ETHER
                        ).toBigInteger()
                )
        ).thenReturn(rawTransaction)
        whenever(ethDataManager.signEthTransaction(rawTransaction, ecKey))
                .thenReturn(Observable.just(signedTx))
        whenever(ethDataManager.pushEthTx(signedTx)).thenReturn(Observable.just(txHash))
        whenever(ethDataManager.setLastTxHashObservable(eq(txHash), any()))
                .thenReturn(Observable.just(txHash))

        whenever(shapeShiftDataManager.addTradeToList(any())).thenReturn(Completable.complete())
        // Act
        subject.onAcceptTermsClicked()
        subject.onConfirmClicked()
        // Assert
        verify(payloadDataManager, times(2)).isDoubleEncrypted
        verify(payloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(ethDataManager).fetchEthAddress()
        verify(ethDataManager).getEthResponseModel()
        verify(ethDataManager).createEthTransaction(
                nonce,
                fromEth.depositAddress,
                Convert.toWei(BigDecimal(fromEth.gasPrice), Convert.Unit.GWEI).toBigInteger(),
                fromEth.gasLimit,
                Convert.toWei(
                        fromEth.depositAmount,
                        Convert.Unit.ETHER
                ).toBigInteger()
        )
        verify(ethDataManager).signEthTransaction(rawTransaction, ecKey)
        verify(ethDataManager).pushEthTx(signedTx)
        verify(ethDataManager).setLastTxHashObservable(eq(txHash), any())
        verifyNoMoreInteractions(ethDataManager)
        verify(ethereumAccountWrapper).deriveECKey(deterministicKey, 0)
        verifyNoMoreInteractions(ethereumAccountWrapper)
        verify(shapeShiftDataManager).addTradeToList(any())
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).setButtonState(true)
        verify(view, atLeastOnce()).shapeShiftData
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).launchProgressPage(fromEth.depositAddress)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onConfirmClicked from BCH`() {
        // Arrange
        val account: Account = mock()
        val outputs: UnspentOutputs = mock()
        val spendableUnspentOutputs: SpendableUnspentOutputs = mock()
        val ecKey: ECKey = mock()
        val txHash = "TX_HASH"
        whenever(view.shapeShiftData).thenReturn(fromBch)
        whenever(payloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(payloadDataManager.getAccountForXPub(fromBch.xPub)).thenReturn(account)
        whenever(bchDataManager.getAddressBalance(fromBch.xPub)).thenReturn(BigInteger.ONE)
        whenever(sendDataManager.getUnspentBchOutputs(fromBch.xPub))
                .thenReturn(Observable.just(outputs))
        whenever(sendDataManager.getSpendableCoins(any(), any(), any()))
                .thenReturn(spendableUnspentOutputs)
        whenever(payloadDataManager.getHDKeysForSigning(account, spendableUnspentOutputs))
                .thenReturn(listOf(ecKey))
        whenever(
                sendDataManager.submitBchPayment(
                        spendableUnspentOutputs,
                        listOf(ecKey),
                        fromBch.depositAddress,
                        fromBch.changeAddress,
                        fromBch.transactionFee,
                        fromBch.depositAmount.multiply(BigDecimal.valueOf(1e8)).toBigInteger()
                )
        ).thenReturn(Observable.just(txHash))
        whenever(shapeShiftDataManager.addTradeToList(any())).thenReturn(Completable.complete())
        // Act
        subject.onAcceptTermsClicked()
        subject.onConfirmClicked()
        // Assert
        verify(payloadDataManager, times(2)).isDoubleEncrypted
        verify(payloadDataManager).getAccountForXPub(fromBch.xPub)
        verify(payloadDataManager).getHDKeysForSigning(account, spendableUnspentOutputs)
        verifyNoMoreInteractions(payloadDataManager)
        verify(bchDataManager).getAddressBalance(fromBch.xPub)
        verifyNoMoreInteractions(bchDataManager)
        verify(sendDataManager).getUnspentBchOutputs(fromBch.xPub)
        verify(sendDataManager).getSpendableCoins(any(), any(), any())
        verify(sendDataManager).submitBchPayment(
                spendableUnspentOutputs,
                listOf(ecKey),
                fromBch.depositAddress,
                fromBch.changeAddress,
                fromBch.transactionFee,
                fromBch.depositAmount.multiply(BigDecimal.valueOf(1e8)).toBigInteger()
        )
        verifyNoMoreInteractions(sendDataManager)
        verify(shapeShiftDataManager).addTradeToList(any())
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).setButtonState(true)
        verify(view, atLeastOnce()).shapeShiftData
        verify(view).showProgressDialog(any())
        verify(view).dismissProgressDialog()
        verify(view).launchProgressPage(fromBch.depositAddress)
        verifyNoMoreInteractions(view)
    }

    private fun initFramework() {
        BlockchainFramework.init(object : FrameworkInterface {
            override fun getRetrofitShapeShiftInstance(): Retrofit {
                throw NotImplementedException("Function should not be called")
            }

            override fun getDevice(): String {
                throw NotImplementedException("Function should not be called")
            }

            override fun getRetrofitExplorerInstance(): Retrofit {
                throw NotImplementedException("Function should not be called")
            }

            override fun getEnvironment(): Environment {
                throw NotImplementedException("Function should not be called")
            }

            override fun getRetrofitApiInstance(): Retrofit {
                throw NotImplementedException("Function should not be called")
            }

            override fun getApiCode(): String {
                throw NotImplementedException("Function should not be called")
            }

            override fun getAppVersion(): String {
                throw NotImplementedException("Function should not be called")
            }

            override fun getBitcoinParams(): NetworkParameters {
                return BitcoinMainNetParams.get()
            }

            override fun getBitcoinCashParams(): NetworkParameters {
                return BitcoinCashMainNetParams.get()
            }
        })
    }

}