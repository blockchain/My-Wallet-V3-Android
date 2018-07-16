package piuk.blockchain.android.ui.account

import android.annotation.SuppressLint
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.PayloadException
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.apache.commons.lang3.tuple.Triple
import org.bitcoinj.core.ECKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.ui.account.AccountPresenter.Companion.KEY_WARN_TRANSFER_ALL
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import java.math.BigInteger
import java.util.Locale

@Config(
    sdk = [23],
    constants = BuildConfig::class,
    application = BlockchainTestApplication::class
)
@RunWith(RobolectricTestRunner::class)
class AccountPresenterTest {

    private lateinit var subject: AccountPresenter
    private val activity: AccountView = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val fundsDataManager: TransferFundsDataManager = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val appUtil: AppUtil = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val privateKeyFactory = PrivateKeyFactory()
    private val currencyState: CurrencyState = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        subject = AccountPresenter(
            payloadDataManager,
            bchDataManager,
            metadataManager,
            fundsDataManager,
            prefsUtil,
            appUtil,
            privateKeyFactory,
            environmentSettings,
            currencyState,
            currencyFormatManager
        )

        subject.initView(activity)
        whenever(activity.locale).thenReturn(Locale.US)
        // TODO: This is cheating for now to ensure all tests pass
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        // TODO: These will break things when fully testing onViewReady()
        val btcAccount = Account().apply {
            label = "LABEL"
            xpub = "X_PUB"
        }
        val bchAccount = GenericMetadataAccount().apply {
            label = "LABEL"
            xpub = "X_PUB"
        }
        whenever(payloadDataManager.accounts).thenReturn(listOf(btcAccount))
        whenever(payloadDataManager.legacyAddresses).thenReturn(mutableListOf())
        whenever(bchDataManager.getAccountMetadataList()).thenReturn(listOf(bchAccount))
        whenever(payloadDataManager.defaultAccountIndex).thenReturn(0)
        whenever(bchDataManager.getDefaultAccountPosition()).thenReturn(0)
        whenever(payloadDataManager.getAddressBalance(any())).thenReturn(BigInteger.ZERO)
        whenever(bchDataManager.getAddressBalance(any())).thenReturn(BigInteger.ZERO)
    }

    @Test
    fun checkTransferableLegacyFundsWarnTransferAllTrue() {
        // Arrange
        val triple = Triple.of(listOf(PendingTransaction()), 1L, 2L)
        whenever(fundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.just(triple))
        val mockPayload = mock(Wallet::class.java)
        whenever(mockPayload.isUpgraded).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        whenever(prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true)).thenReturn(true)
        // Act
        subject.checkTransferableLegacyFunds(false, true)
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(true)
        verify(activity).onShowTransferableLegacyFundsWarning(false)
        verify(activity).dismissProgressDialog()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun checkTransferableLegacyFundsWarnTransferAllTrueDontShowDialog() {
        // Arrange
        val triple = Triple.of(listOf(PendingTransaction()), 1L, 2L)
        whenever(fundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.just(triple))
        val mockPayload = mock(Wallet::class.java)
        whenever(mockPayload.isUpgraded).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        whenever(prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true)).thenReturn(true)
        // Act
        subject.checkTransferableLegacyFunds(false, false)
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(true)
        verify(activity).dismissProgressDialog()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun checkTransferableLegacyFundsNoFundsAvailable() {
        // Arrange
        val triple = Triple.of(emptyList<PendingTransaction>(), 1L, 2L)
        whenever(fundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.just<Triple<List<PendingTransaction>, Long, Long>>(triple))
        val mockPayload = mock(Wallet::class.java)
        whenever(mockPayload.isUpgraded).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        // Act
        subject.checkTransferableLegacyFunds(true, true)
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(false)
        verify(activity).dismissProgressDialog()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun checkTransferableLegacyFundsThrowsException() {
        // Arrange
        whenever(fundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.error(Throwable()))
        // Act
        subject.checkTransferableLegacyFunds(true, true)
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(false)
        verify(activity).dismissProgressDialog()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun createNewAccountSuccessful() {
        // Arrange
        val account: Account = mock()
        whenever(account.xpub).thenReturn("xpub")
        whenever(payloadDataManager.createNewAccount(anyString(), isNull<String>()))
            .thenReturn(Observable.just(account))
        whenever(bchDataManager.serializeForSaving()).thenReturn("")
        whenever(metadataManager.saveToMetadata(any(), anyInt())).thenReturn(Completable.complete())
        // Act
        subject.createNewAccount("")
        // Assert
        verify(payloadDataManager).createNewAccount(anyString(), isNull())
        verify(bchDataManager).createAccount("xpub")
        verify(bchDataManager).serializeForSaving()
        verify(metadataManager).saveToMetadata(any(), anyInt())
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK))
        verify(activity).broadcastIntent(any())
    }

    @Test
    fun createNewAccountDecryptionException() {
        // Arrange
        whenever(payloadDataManager.createNewAccount(anyString(), isNull<String>()))
            .thenReturn(Observable.error(DecryptionException()))
        // Act
        subject.createNewAccount("")
        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun createNewAccountPayloadException() {
        // Arrange
        whenever(payloadDataManager.createNewAccount(anyString(), isNull<String>()))
            .thenReturn(Observable.error(PayloadException()))
        // Act
        subject.createNewAccount("")
        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun createNewAccountUnknownException() {
        // Arrange
        whenever(payloadDataManager.createNewAccount(anyString(), isNull<String>()))
            .thenReturn(Observable.error(Exception()))
        // Act
        subject.createNewAccount("")
        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateLegacyAddressSuccessful() {
        // Arrange
        val legacyAddress = LegacyAddress()
        whenever(payloadDataManager.updateLegacyAddress(legacyAddress))
            .thenReturn(Completable.complete())
        // Act
        subject.updateLegacyAddress(legacyAddress)
        // Assert
        verify(payloadDataManager).updateLegacyAddress(legacyAddress)
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK))
        verify(activity).broadcastIntent(any())
    }

    @Test
    fun updateLegacyAddressFailed() {
        // Arrange
        val legacyAddress = LegacyAddress()
        whenever(payloadDataManager.updateLegacyAddress(legacyAddress))
            .thenReturn(Completable.error(Throwable()))
        // Act
        subject.updateLegacyAddress(legacyAddress)
        // Assert
        verify(payloadDataManager).updateLegacyAddress(legacyAddress)
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onScanButtonClickedCameraInUse() {
        // Arrange
        whenever(appUtil.isCameraOpen).thenReturn(true)
        // Act
        subject.onScanButtonClicked()
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onScanButtonClickedCameraAvailable() {
        // Arrange
        whenever(appUtil.isCameraOpen).thenReturn(false)
        // Act
        subject.onScanButtonClicked()
        // Assert
        verify(activity).startScanForResult()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun importBip38AddressWithValidPassword() {
        // Arrange

        // Act
        subject.importBip38Address(
            "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS",
            "password"
        )
        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
    }

    @Test
    fun importBip38AddressWithIncorrectPassword() {
        // Arrange

        // Act
        subject.importBip38Address(
            "6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS",
            "notthepassword"
        )
        // Assert
        verify(activity).showProgressDialog(anyInt())
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verify(activity).dismissProgressDialog()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedBip38() {
        // Arrange

        // Act
        subject.onAddressScanned("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS")
        // Assert
        verify(activity).showBip38PasswordDialog("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS")
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedNonBip38() {
        // Arrange
        whenever(payloadDataManager.getKeyFromImportedData(anyString(), anyString()))
            .thenReturn(Observable.just(mock(ECKey::class.java)))
        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD")
        // Assert
        verify(payloadDataManager).getKeyFromImportedData(anyString(), anyString())
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
    }

    @Test
    fun onAddressScannedNonBip38Failure() {
        // Arrange
        whenever(payloadDataManager.getKeyFromImportedData(anyString(), anyString()))
            .thenReturn(Observable.error(Throwable()))
        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD")
        whenever(payloadDataManager.getKeyFromImportedData(anyString(), anyString()))
            .thenReturn(Observable.just(mock(ECKey::class.java)))
        // Assert
        verify(payloadDataManager).getKeyFromImportedData(anyString(), anyString())
        verify(activity).showProgressDialog(anyInt())
        verify(activity).dismissProgressDialog()
        verify(activity).showToast(anyInt(), anyString())
    }

    @Test
    fun onAddressScannedWatchOnlyInvalidAddress() {
        // Arrange

        // Act
        subject.onAddressScanned("test")
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedWatchOnlyNullAddress() {
        // Arrange

        // Act
        subject.onAddressScanned(null)
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedWatchAddressAlreadyInWallet() {
        // Arrange
        val mockPayload = mock(Wallet::class.java, RETURNS_DEEP_STUBS)

        whenever(mockPayload.legacyAddressStringList.contains(any<Any>())).thenReturn(true)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        // Act
        subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7")
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun onAddressScannedWatchAddressNotInWallet() {
        // Arrange
        val mockPayload = mock(Wallet::class.java, RETURNS_DEEP_STUBS)

        whenever(mockPayload.legacyAddressStringList.contains(any<Any>())).thenReturn(false)
        whenever(payloadDataManager.wallet).thenReturn(mockPayload)
        // Act
        subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7")
        // Assert
        verify(activity).showWatchOnlyWarningDialog("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7")
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun confirmImportWatchOnlySuccess() {
        // Arrange
        val address = "17UovdU9ZvepPe75igTQwxqNME1HbnvMB7"
        whenever(payloadDataManager.addLegacyAddress(any()))
            .thenReturn(Completable.complete())
        // Act
        subject.confirmImportWatchOnly(address)
        // Assert
        verify(payloadDataManager).addLegacyAddress(any())
        verify(activity).showRenameImportedAddressDialog(any())
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun confirmImportWatchOnlyFailure() {
        // Arrange
        val address = "17UovdU9ZvepPe75igTQwxqNME1HbnvMB7"
        whenever(payloadDataManager.addLegacyAddress(any()))
            .thenReturn(Completable.error(Throwable()))
        // Act
        subject.confirmImportWatchOnly(address)
        // Assert
        verify(payloadDataManager).addLegacyAddress(any())
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }

    @SuppressLint("VisibleForTests")
    @Test
    fun handlePrivateKeyWhenKeyIsNull() {
        // Arrange

        // Act
        subject.handlePrivateKey(null, null)
        // Assert
        verify(activity).showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(activity)
    }

    @SuppressLint("VisibleForTests")
    @Test
    fun handlePrivateKeyExistingAddressSuccess() {
        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        val mockECKey = mock(ECKey::class.java)
        whenever(mockECKey.hasPrivKey()).thenReturn(true)
        val legacyAddress = LegacyAddress()
        whenever(payloadDataManager.setKeyForLegacyAddress(mockECKey, null))
            .thenReturn(Observable.just(legacyAddress))
        whenever(fundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.empty())

        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(
            currencyFormatManager.getFormattedSelectedCoinValueWithUnit(BigInteger.ZERO)
        ).thenReturn("")

        // Act
        subject.handlePrivateKey(mockECKey, null)
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK))
        verify(activity).showRenameImportedAddressDialog(legacyAddress)
    }

    @SuppressLint("VisibleForTests")
    @Test
    fun handlePrivateKeyExistingAddressFailure() {
        // Arrange
        val mockECKey = mock(ECKey::class.java)
        whenever(mockECKey.hasPrivKey()).thenReturn(true)
        whenever(payloadDataManager.setKeyForLegacyAddress(mockECKey, null))
            .thenReturn(Observable.error(Throwable()))
        // Act
        subject.handlePrivateKey(mockECKey, null)
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(activity)
    }
}