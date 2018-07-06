package piuk.blockchain.android.ui.swipetoreceive

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.Balance
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import piuk.blockchain.android.R
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_ACCOUNT_NAME
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_ADDRESSES
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_BCH_ACCOUNT_NAME
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_BCH_ADDRESSES
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_ETH_ADDRESS
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.math.BigInteger
import java.util.LinkedHashMap

class SwipeToReceiveHelperTest : RxTest() {

    private lateinit var subject: SwipeToReceiveHelper
    private val payloadDataManager: PayloadDataManager = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val stringUtils: StringUtils = mock()
    private val ethDataManager: EthDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val bchDataManager: BchDataManager = mock()
    private val environmentSettings: EnvironmentConfig = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())

        subject = SwipeToReceiveHelper(
            payloadDataManager,
            prefsUtil,
            ethDataManager,
            bchDataManager,
            stringUtils,
            environmentSettings
        )
    }

    @Test
    @Throws(Exception::class)
    fun updateAndStoreBitcoinAddresses() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true))
            .thenReturn(true)
        val mockAccount: Account = mock()
        whenever(payloadDataManager.defaultAccount).thenReturn(mockAccount)
        whenever(mockAccount.label).thenReturn("Account")
        whenever(payloadDataManager.getReceiveAddressAtPosition(eq(mockAccount), anyInt()))
            .thenReturn("address")
        // Act
        subject.updateAndStoreBitcoinAddresses()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)
        verify(payloadDataManager, times(5)).getReceiveAddressAtPosition(eq(mockAccount), anyInt())
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "Account")
        verify(prefsUtil).setValue(
            KEY_SWIPE_RECEIVE_ADDRESSES,
            "address,address,address,address,address,"
        )
    }

    @Test
    @Throws(Exception::class)
    fun updateAndStoreBitcoinCashAddresses() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true))
            .thenReturn(true)
        val mockAccount: GenericMetadataAccount = mock()
        whenever(bchDataManager.getDefaultGenericMetadataAccount()).thenReturn(mockAccount)
        whenever(bchDataManager.getDefaultAccountPosition()).thenReturn(0)
        whenever(mockAccount.label).thenReturn("BCH Account")
        whenever(bchDataManager.getReceiveAddressAtPosition(eq(0), anyInt()))
            .thenReturn("1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu")
        // Act
        subject.updateAndStoreBitcoinCashAddresses()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)
        verify(bchDataManager, times(5)).getReceiveAddressAtPosition(eq(0), anyInt())
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_BCH_ACCOUNT_NAME, "BCH Account")
        verify(prefsUtil).setValue(
            KEY_SWIPE_RECEIVE_BCH_ADDRESSES,
            "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu," +
                "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu," +
                "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu," +
                "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu," +
                "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu,"
        )
    }

    @Test
    @Throws(Exception::class)
    fun storeEthAddress() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true))
            .thenReturn(true)
        whenever(ethDataManager.getEthWallet()?.account?.address).thenReturn("address")
        // Act
        subject.storeEthAddress()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)
        verify(ethDataManager, atLeastOnce()).getEthWallet()
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, "address")
    }

    @Test
    @Throws(Exception::class)
    fun getNextAvailableAddressSingleValid() {
        // Arrange
        val map = LinkedHashMap<String, Balance>()
        val balance0 = Balance().apply { finalBalance = BigInteger.valueOf(1000L) }
        val balance1 = Balance().apply { finalBalance = BigInteger.valueOf(5L) }
        val balance2 = Balance().apply { finalBalance = BigInteger.valueOf(-10L) }
        val balance3 = Balance().apply { finalBalance = BigInteger.valueOf(0L) }
        val balance4 = Balance().apply { finalBalance = BigInteger.valueOf(0L) }
        map["addr0"] = balance0
        map["addr1"] = balance1
        map["addr2"] = balance2
        map["addr3"] = balance3
        map["addr4"] = balance4
        whenever(payloadDataManager.getBalanceOfAddresses(anyList()))
            .thenReturn(Observable.just(map))
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
            .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val testObserver = subject.getNextAvailableBitcoinAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("addr3")
    }

    @Test
    @Throws(Exception::class)
    fun getNextAvailableAddressSingleAllUsed() {
        // Arrange
        val map = LinkedHashMap<String, Balance>()
        val balance0 = Balance().apply { finalBalance = BigInteger.valueOf(1000L) }
        val balance1 = Balance().apply { finalBalance = BigInteger.valueOf(5L) }
        val balance2 = Balance().apply { finalBalance = BigInteger.valueOf(-10L) }
        val balance3 = Balance().apply { finalBalance = BigInteger.valueOf(1L) }
        val balance4 = Balance().apply { finalBalance = BigInteger.valueOf(1_000_000_000_000L) }
        map["addr0"] = balance0
        map["addr1"] = balance1
        map["addr2"] = balance2
        map["addr3"] = balance3
        map["addr4"] = balance4
        whenever(payloadDataManager.getBalanceOfAddresses(anyList()))
            .thenReturn(Observable.just(map))
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
            .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val testObserver = subject.getNextAvailableBitcoinAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("")
    }

    @Test
    @Throws(Exception::class)
    fun `getNextAvailableBCHAddressSingle Valid`() {
        // Arrange
        val map = LinkedHashMap<String, Balance>()
        val balance0 = Balance().apply { finalBalance = BigInteger.valueOf(1000L) }
        val balance1 = Balance().apply { finalBalance = BigInteger.valueOf(5L) }
        val balance2 = Balance().apply { finalBalance = BigInteger.valueOf(-10L) }
        val balance3 = Balance().apply { finalBalance = BigInteger.valueOf(0L) }
        val balance4 = Balance().apply { finalBalance = BigInteger.valueOf(0L) }
        map["1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu"] = balance0
        map["1KXrWXciRDZUpQwQmuM1DbwsKDLYAYsVLR"] = balance1
        map["16w1D5WRVKJuZUsSRzdLp9w3YGcgoxDXb"] = balance2
        map["1DJk1Feuabguw5CW9CGQRQ3U1pp5Pbn3HK"] = balance3
        map["1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"] = balance4
        whenever(payloadDataManager.getBalanceOfBchAddresses(anyList()))
            .thenReturn(Observable.just(map))
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_BCH_ADDRESSES, ""))
            .thenReturn(
                "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu," +
                    "1KXrWXciRDZUpQwQmuM1DbwsKDLYAYsVLR," +
                    "16w1D5WRVKJuZUsSRzdLp9w3YGcgoxDXb," +
                    "1DJk1Feuabguw5CW9CGQRQ3U1pp5Pbn3HK," +
                    "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
            )
        // Act
        val testObserver = subject.getNextAvailableBitcoinCashAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("bitcoincash:qzr0c757kuat5zh2nradx0wd5edz33avqqpae2pxhv")
    }

    @Test
    @Throws(Exception::class)
    fun `getNextAvailableBCHAddressSingle All Used`() {
        // Arrange
        val map = LinkedHashMap<String, Balance>()
        val balance0 = Balance().apply { finalBalance = BigInteger.valueOf(1000L) }
        val balance1 = Balance().apply { finalBalance = BigInteger.valueOf(5L) }
        val balance2 = Balance().apply { finalBalance = BigInteger.valueOf(-10L) }
        val balance3 = Balance().apply { finalBalance = BigInteger.valueOf(1L) }
        val balance4 = Balance().apply { finalBalance = BigInteger.valueOf(1_000_000_000_000L) }
        map["1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu"] = balance0
        map["1KXrWXciRDZUpQwQmuM1DbwsKDLYAYsVLR"] = balance1
        map["16w1D5WRVKJuZUsSRzdLp9w3YGcgoxDXb"] = balance2
        map["1DJk1Feuabguw5CW9CGQRQ3U1pp5Pbn3HK"] = balance3
        map["1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"] = balance4
        whenever(payloadDataManager.getBalanceOfBchAddresses(anyList()))
            .thenReturn(Observable.just(map))
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_BCH_ADDRESSES, ""))
            .thenReturn(
                "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu," +
                    "1KXrWXciRDZUpQwQmuM1DbwsKDLYAYsVLR," +
                    "16w1D5WRVKJuZUsSRzdLp9w3YGcgoxDXb," +
                    "1DJk1Feuabguw5CW9CGQRQ3U1pp5Pbn3HK," +
                    "1ATy3ktyaYjzZZQQnhvPsuBVheUDYcUP7V"
            )
        // Act
        val testObserver = subject.getNextAvailableBitcoinCashAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("")
    }

    @Test
    @Throws(Exception::class)
    fun getEthReceiveAddressSingle() {
        // Arrange
        val address = "ADDRESS"
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null))
            .thenReturn(address)
        // Act
        val testObserver = subject.getEthReceiveAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinReceiveAddresses() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
            .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val result = subject.getBitcoinReceiveAddresses()
        // Assert
        assertEquals(5, result.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinCashReceiveAddressesEmptyList() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
            .thenReturn("")
        // Act
        val result = subject.getBitcoinReceiveAddresses()
        // Assert
        assertEquals(emptyList<Any>(), result)
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinCashReceiveAddresses() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_BCH_ADDRESSES, ""))
            .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val result = subject.getBitcoinCashReceiveAddresses()
        // Assert
        assertEquals(5, result.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinReceiveAddressesEmptyList() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_BCH_ADDRESSES, ""))
            .thenReturn("")
        // Act
        val result = subject.getBitcoinCashReceiveAddresses()
        // Assert
        assertEquals(emptyList<Any>(), result)
    }

    @Test
    @Throws(Exception::class)
    fun getEthReceiveAddress() {
        // Arrange
        val address = "ADDRESS"
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null))
            .thenReturn(address)
        // Act
        val result = subject.getEthReceiveAddress()
        // Assert
        verify(prefsUtil).getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null)
        result `should equal` address
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinAccountName() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, ""))
            .thenReturn("Account")
        // Act
        val result = subject.getBitcoinAccountName()
        // Assert
        result `should equal` "Account"
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinCashAccountName() {
        // Arrange
        val defaultAccountName = "Default account name"
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_BCH_ACCOUNT_NAME, defaultAccountName))
            .thenReturn("Account")
        whenever(stringUtils.getString(R.string.bch_default_account_label))
            .thenReturn(defaultAccountName)
        // Act
        val result = subject.getBitcoinCashAccountName()
        // Assert
        result `should equal` "Account"
    }

    @Test
    @Throws(Exception::class)
    fun getEthAccountName() {
        // Arrange
        val label = "LABEL"
        whenever(stringUtils.getString(R.string.eth_default_account_label)).thenReturn(label)
        // Act
        val result = subject.getEthAccountName()
        // Assert
        result `should equal` label
    }
}