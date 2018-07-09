package piuk.blockchain.android.ui.receive

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.AddressBook
import info.blockchain.wallet.payload.data.LegacyAddress
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.BTCDenomination
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.currency.ETHDenomination
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import java.math.BigDecimal
import java.math.BigInteger

class WalletAccountHelperTest {

    private lateinit var subject: WalletAccountHelper
    private val payloadManager: PayloadManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val stringUtils: StringUtils = mock()
    private val currencyState: CurrencyState = mock()
    private val ethDataManager: EthDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val bchDataManager: BchDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val environmentSettings: EnvironmentConfig = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()

    @Before
    fun setUp() {
        subject = WalletAccountHelper(
            payloadManager,
            stringUtils,
            currencyState,
            ethDataManager,
            bchDataManager,
            environmentSettings,
            currencyFormatManager
        )

        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
        whenever(environmentSettings.bitcoinNetworkParameters)
            .thenReturn(BitcoinMainNetParams.get())
    }

    @Test
    fun `getAccountItems should return one Account and one LegacyAddress`() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        val address = "ADDRESS"
        val account = Account().apply {
            this.label = label
            this.xpub = xPub
        }
        val legacyAddress = LegacyAddress().apply {
            this.label = null
            this.address = address
        }
        whenever(payloadManager.payload.hdWallets[0].accounts).thenReturn(listOf(account))
        whenever(payloadManager.payload.legacyAddressList).thenReturn(mutableListOf(legacyAddress))
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(payloadManager.getAddressBalance(xPub)).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result.size `should be` 2
        result[0].accountObject `should equal` account
        result[1].accountObject `should equal` legacyAddress
        verify(currencyFormatManager, atLeastOnce()).getFormattedBtcValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }

    @Test
    fun `getAccountItems when currency is BCH should return one Account and one LegacyAddress`() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        // Must be valid or conversion to BECH32 will fail
        val address = "17MgvXUa6tPsh3KMRWAPYBuDwbtCBF6Py5"
        val account = GenericMetadataAccount().apply {
            this.label = label
            this.xpub = xPub
        }
        val legacyAddress = LegacyAddress().apply {
            this.label = null
            this.address = address
        }
        whenever(bchDataManager.getActiveAccounts()).thenReturn(listOf(account))
        whenever(bchDataManager.getAddressBalance(address)).thenReturn(BigInteger.TEN)
        whenever(payloadManager.payload.legacyAddressList).thenReturn(mutableListOf(legacyAddress))
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BCH)
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(bchDataManager.getAddressBalance(xPub)).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        verify(bchDataManager).getActiveAccounts()
        verify(bchDataManager, atLeastOnce()).getAddressBalance(address)
        result.size `should be` 2
        result[0].accountObject `should equal` account
        result[1].accountObject `should equal` legacyAddress
        verify(currencyFormatManager, atLeastOnce()).getFormattedBchValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }

    @Test
    fun `getHdAccounts should return single Account`() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        val archivedAccount = Account().apply { isArchived = true }
        val account = Account().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(payloadManager.payload.hdWallets[0].accounts)
            .thenReturn(mutableListOf(archivedAccount, account))
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(payloadManager.getAddressBalance(xPub)).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result.size `should equal` 1
        result[0].accountObject `should be` account
        verify(currencyFormatManager, atLeastOnce()).getFormattedBtcValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }

    @Test
    fun `getHdAccounts when currency is BCH should return single Account`() {
        // Arrange
        val label = "LABEL"
        val xPub = "X_PUB"
        val archivedAccount = GenericMetadataAccount().apply { isArchived = true }
        val account = GenericMetadataAccount().apply {
            this.label = label
            this.xpub = xPub
        }
        whenever(bchDataManager.getActiveAccounts())
            .thenReturn(mutableListOf(archivedAccount, account))
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BCH)
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(bchDataManager.getAddressBalance(xPub)).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(bchDataManager).getActiveAccounts()
        result.size `should equal` 1
        result[0].accountObject `should be` account
        verify(currencyFormatManager, atLeastOnce()).getFormattedBchValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }

    @Test
    fun `getAccountItems when currency is ETH should return one account`() {
        // Arrange
        val ethAccount: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        whenever(ethDataManager.getEthWallet()?.account).thenReturn(ethAccount)
        whenever(ethAccount.address).thenReturn("address")
        whenever(ethDataManager.getEthResponseModel()).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(1234567890L))
        // Act
        val result = subject.getAccountItems()
        // Assert
        verify(ethDataManager, atLeastOnce()).getEthWallet()
        result.size `should be` 1
        result[0].accountObject `should equal` ethAccount
        verify(currencyFormatManager, atLeastOnce()).getFormattedEthShortValueWithUnit(
            BigDecimal.valueOf(1234567890L), ETHDenomination.WEI
        )
    }

    @Test
    fun `getLegacyAddresses should return single LegacyAddress`() {
        // Arrange
        val address = "ADDRESS"
        val archivedAddress = LegacyAddress().apply { tag = LegacyAddress.ARCHIVED_ADDRESS }
        val legacyAddress = LegacyAddress().apply {
            this.label = null
            this.address = address
        }
        whenever(payloadManager.payload.legacyAddressList)
            .thenReturn(mutableListOf(archivedAddress, legacyAddress))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(payloadManager.getAddressBalance(address)).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getLegacyAddresses()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result.size `should equal` 1
        result[0].accountObject `should be` legacyAddress
        verify(currencyFormatManager, atLeastOnce()).getFormattedBtcValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }

    @Test
    fun `getAddressBookEntries should return single item`() {
        // Arrange
        val addressBook = AddressBook()
        whenever(payloadManager.payload.addressBook).thenReturn(listOf(addressBook))
        // Act
        val result = subject.getAddressBookEntries()
        // Assert
        result.size `should equal` 1
    }

    @Test
    fun `getAddressBookEntries should return empty list`() {
        // Arrange
        whenever(payloadManager.payload.addressBook)
            .thenReturn(null)
        // Act
        val result = subject.getAddressBookEntries()
        // Assert
        result.size `should equal` 0
    }

    @Test
    fun `getDefaultAccount should return ETH account`() {
        // Arrange
        val ethAccount: EthereumAccount = mock()
        val combinedEthModel: CombinedEthModel = mock()
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        whenever(ethDataManager.getEthWallet()?.account).thenReturn(ethAccount)
        whenever(ethAccount.address).thenReturn("address")
        whenever(ethDataManager.getEthResponseModel()).thenReturn(combinedEthModel)
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(1234567890L))
        // Act
        val result = subject.getDefaultAccount()
        // Assert
        verify(ethDataManager, atLeastOnce()).getEthWallet()
        result.accountObject `should equal` ethAccount
        verify(currencyFormatManager, atLeastOnce()).getFormattedEthShortValueWithUnit(
            BigDecimal.valueOf(1234567890L), ETHDenomination.WEI
        )
    }

    @Test
    fun `getDefaultAccount should return BTC account`() {
        // Arrange
        val btcAccount: Account = mock()
        whenever(btcAccount.xpub).thenReturn("xpub")
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        whenever(payloadManager.payload.hdWallets[0].defaultAccountIdx).thenReturn(0)
        whenever(payloadManager.payload.hdWallets[0].accounts[0]).thenReturn(btcAccount)
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(payloadManager.getAddressBalance("xpub")).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getDefaultAccount()
        // Assert
        verify(payloadManager, atLeastOnce()).payload
        result.accountObject `should equal` btcAccount
        verify(currencyFormatManager, atLeastOnce()).getFormattedBtcValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }

    @Test
    fun `getDefaultAccount should return BCH account`() {
        // Arrange
        val bchAccount: GenericMetadataAccount = mock()
        whenever(bchAccount.xpub).thenReturn("")
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BCH)
        whenever(bchDataManager.getDefaultGenericMetadataAccount()).thenReturn(bchAccount)
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(bchDataManager.getAddressBalance("")).thenReturn(BigInteger.TEN)
        // Act
        val result = subject.getDefaultAccount()
        // Assert
        verify(bchDataManager).getDefaultGenericMetadataAccount()
        result.accountObject `should equal` bchAccount
        verify(currencyFormatManager, atLeastOnce()).getFormattedBchValueWithUnit(
            BigDecimal.TEN, BTCDenomination.SATOSHI
        )
    }
}