package piuk.blockchain.android.coincore.erc20

import com.blockchain.android.testutils.rxInit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.testutils.gwei
import com.blockchain.testutils.usdPax
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeLimits
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import piuk.blockchain.android.coincore.BlockchainAccount
import kotlin.test.assertEquals
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

@Suppress("UnnecessaryVariable") class Erc20OnChainTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val ethDataManager: EthDataManager = mock {
        on { erc20ContractAddress(any()) } itReturns ""
    }
    private val ethFeeOptions: FeeOptions = mock()

    private val feeManager: FeeDataManager = mock {
        on { getErc20FeeOptions(any()) } itReturns Observable.just(ethFeeOptions)
    }
    private val walletPreferences: WalletStatus = mock {
        on { getFeeTypeForAsset(ASSET) } itReturns FeeLevel.Regular.ordinal
    }
    private val exchangeRates: ExchangeRateDataManager = mock()

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }

    private val subject = Erc20OnChainTxEngine(
        ethDataManager = ethDataManager,
        feeManager = feeManager,
        requireSecondPassword = false,
        walletPreferences = walletPreferences
    )

    @Before
    fun setup() {
        injectMocks(
            module {
                scope(payloadScopeQualifier) {
                    factory {
                        currencyPrefs
                    }
                }
            }
        )
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
            on { address } itReturns TARGET_ADDRESS
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset
        verify(txTarget).address

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount = mock<Erc20NonCustodialAccount>() {
            on { asset } itReturns WRONG_ASSET
        }
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
            on { address } itReturns TARGET_ADDRESS
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, ASSET)
        verify(sourceAccount).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(ASSET) &&
                it.totalBalance == CryptoValue.zero(ASSET) &&
                it.availableBalance == CryptoValue.zero(ASSET) &&
                it.feeAmount == CryptoValue.zero(FEE_ASSET) &&
                it.selectedFiat == SELECTED_FIAT &&
                it.confirmations.isEmpty() &&
                it.minLimit == null &&
                it.maxLimit == null &&
                it.validationState == ValidationState.UNINITIALISED &&
                it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(walletPreferences).getFeeTypeForAsset(ASSET)
        verify(currencyPrefs).selectedFiatCurrency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for regular fees`() {
        // Arrange
        val totalBalance = 21.usdPax()
        val actionableBalance = 20.usdPax()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        val inputAmount = 2.usdPax()
        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val expectedFullFee = expectedFee

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == actionableBalance &&
                it.feeForFullAvailable == expectedFullFee &&
                it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        verify(sourceAccount, atLeastOnce()).asset
        verify(sourceAccount).accountBalance
        verify(sourceAccount).actionableBalance
        verify(ethDataManager).erc20ContractAddress(any())
        verify(feeManager).getErc20FeeOptions(any())
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).regularFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for priority fees`() {
        // Arrange
        val totalBalance = 21.usdPax()
        val actionableBalance = 20.usdPax()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(CryptoCurrency.ETHER),
            totalBalance = CryptoValue.zero(CryptoCurrency.ETHER),
            availableBalance = CryptoValue.zero(CryptoCurrency.ETHER),
            feeForFullAvailable = CryptoValue.zero(CryptoCurrency.ETHER),
            feeAmount = CryptoValue.zero(CryptoCurrency.ETHER),
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Priority,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        val inputAmount = 2.usdPax()
        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_PRIORITY).gwei()
        val expectedFullFee = expectedFee

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == actionableBalance &&
                it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).asset
        verify(sourceAccount).accountBalance
        verify(sourceAccount).actionableBalance
        verify(ethDataManager).erc20ContractAddress(any())
        verify(feeManager).getErc20FeeOptions(any())
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).priorityFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to PRIORITY updates the pendingTx correctly`() {
        // Arrange
        val totalBalance = 21.usdPax()
        val availableBalance = 20.usdPax()

        val inputAmount = 2.usdPax()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = regularFee,
            feeAmount = regularFee,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_PRIORITY).gwei()
        val fullFee = expectedFee

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == availableBalance &&
                    it.feeForFullAvailable == fullFee &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).asset
        verify(sourceAccount).accountBalance
        verify(sourceAccount).actionableBalance
        verify(ethDataManager).erc20ContractAddress(any())
        verify(feeManager).getErc20FeeOptions(any())
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).priorityFee
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Priority.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to NONE is rejected`() {
        // Arrange
        val totalBalance = 21.usdPax()
        val availableBalance = 20.usdPax()
        val inputAmount = 2.usdPax()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to CUSTOM is rejected`() {
        // Arrange
        val totalBalance = 21.usdPax()
        val availableBalance = 20.usdPax()
        val inputAmount = 2.usdPax()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            100
        ).test()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to REGULAR has no effect`() {
        // Arrange
        val totalBalance = 21.usdPax()
        val availableBalance = 20.usdPax()
        val inputAmount = 2.usdPax()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == availableBalance &&
                it.feeAmount == regularFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<Erc20NonCustodialAccount> {
        on { asset } itReturns ASSET
        on { accountBalance } itReturns Single.just(totalBalance)
        on { actionableBalance } itReturns Single.just(availableBalance)
    }

    private fun withDefaultFeeOptions() {
        whenever(ethFeeOptions.gasLimit).thenReturn(GAS_LIMIT)
        whenever(ethFeeOptions.priorityFee).thenReturn(FEE_PRIORITY)
        whenever(ethFeeOptions.regularFee).thenReturn(FEE_REGULAR)
        whenever(ethFeeOptions.gasLimitContract).thenReturn(GAS_LIMIT_CONTRACT)
        whenever(ethFeeOptions.limits).thenReturn(FeeLimits(FEE_REGULAR, FEE_PRIORITY))
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection, expectedLevel: FeeLevel) =
        feeSelection.selectedLevel == expectedLevel &&
            feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.asset == CryptoCurrency.ETHER &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(ethFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.PAX
        private val WRONG_ASSET = CryptoCurrency.BTC
        private val FEE_ASSET = CryptoCurrency.ETHER
        private const val TARGET_ADDRESS = "VALID_PAX_ADDRESS"
        private const val GAS_LIMIT = 3000L
        private const val GAS_LIMIT_CONTRACT = 5000L
        private const val FEE_PRIORITY = 5L
        private const val FEE_REGULAR = 2L
        private const val SELECTED_FIAT = "INR"

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}
