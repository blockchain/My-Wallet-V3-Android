package piuk.blockchain.android.ui.createwallet

import android.content.Intent
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil

class CreateWalletPresenterTest {

    private lateinit var subject: CreateWalletPresenter
    private var view: CreateWalletView = mock()
    private var appUtil: AppUtil = mock()
    private var accessState: AccessState = mock()
    private var payloadDataManager: PayloadDataManager =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private var prefsUtil: PrefsUtil = mock()
    private var prngFixer: PrngFixer = mock()

    @Before
    fun setUp() {
        subject =
            CreateWalletPresenter(payloadDataManager, prefsUtil, appUtil, accessState, prngFixer)
        subject.initView(view)
    }

    @Test
    fun onViewReady() {
        // Nothing to test
    }

    @Test
    fun `parseExtras for create`() {
        // Arrange
        val intent: Intent = mock()
        whenever(intent.getStringExtra(RecoverFundsActivity.RECOVERY_PHRASE)).thenReturn("")
        // Act
        subject.parseExtras(intent)
        // Assert
        verify(view).setTitleText(R.string.new_wallet)
        verify(view).setNextText(R.string.create_wallet)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `parseExtras for recover`() {
        // Arrange
        val intent: Intent = mock()
        whenever(intent.getStringExtra(RecoverFundsActivity.RECOVERY_PHRASE))
            .thenReturn("all all all all all all all all all all all all")
        // Act
        subject.parseExtras(intent)
        // Assert
        verify(view).setTitleText(R.string.recover_funds)
        verify(view).setNextText(R.string.dialog_continue)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on weak password`() {
        // Arrange

        // Act
        subject.calculateEntropy("password")
        // Assert
        verify(view).setEntropyStrength(8)
        verify(view).setEntropyLevel(0)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on regular password`() {
        // Arrange

        // Act
        subject.calculateEntropy("MyWallet")
        // Assert
        verify(view).setEntropyStrength(46)
        verify(view).setEntropyLevel(1)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on normal password`() {
        // Arrange

        // Act
        subject.calculateEntropy("MyTestWallet")
        // Assert
        verify(view).setEntropyStrength(69)
        verify(view).setEntropyLevel(2)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on strong password`() {
        // Arrange

        // Act
        subject.calculateEntropy("MyTestWallet!@!ASD@!")
        // Assert
        verify(view).setEntropyStrength(100)
        verify(view).setEntropyLevel(3)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials create wallet`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val pw2 = "MyTestWallet"
        val accountName = "AccountName"
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        whenever(view.getDefaultAccountName()).thenReturn(accountName)
        whenever(payloadDataManager.createHdWallet(any(), any(), any())).thenReturn(
            Observable.just(
                Wallet()
            )
        )
        whenever(payloadDataManager.wallet!!.guid).thenReturn(guid)
        whenever(payloadDataManager.wallet!!.sharedKey).thenReturn(sharedKey)
        // Act
        subject.passwordStrength = 80
        subject.recoveryPhrase = ""
        subject.validateCredentials(email, pw1, pw2)
        // Assert
        verify(prngFixer).applyPRNGFixes()
        val observer = payloadDataManager.createHdWallet(pw1, accountName, email).test()
        observer.assertComplete()
        observer.assertNoErrors()

        verify(view).showProgressDialog(any())
        verify(prefsUtil).setValue(PrefsUtil.KEY_EMAIL, email)
        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, guid)
        verify(accessState).isNewlyCreated = true
        verify(appUtil).sharedKey = sharedKey
        verify(view).startPinEntryActivity()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `validateCredentials restore wallet`() {
        // Arrange
        val email = "john@snow.com"
        val pw1 = "MyTestWallet"
        val pw2 = "MyTestWallet"
        val accountName = "AccountName"
        val sharedKey = "SHARED_KEY"
        val guid = "GUID"
        whenever(view.getDefaultAccountName()).thenReturn(accountName)
        whenever(payloadDataManager.restoreHdWallet(any(), any(), any(), any()))
            .thenReturn(Observable.just(Wallet()))
        whenever(payloadDataManager.wallet!!.guid).thenReturn(guid)
        whenever(payloadDataManager.wallet!!.sharedKey).thenReturn(sharedKey)
        // Act
        subject.passwordStrength = 80
        subject.recoveryPhrase = "all all all all all all all all all all all all"
        subject.validateCredentials(email, pw1, pw2)
        // Assert
        val observer =
            payloadDataManager.restoreHdWallet(email, pw1, accountName, subject.recoveryPhrase)
                .test()
        observer.assertComplete()
        observer.assertNoErrors()

        verify(view).showProgressDialog(any())
        verify(prefsUtil).setValue(PrefsUtil.KEY_EMAIL, email)
        verify(prefsUtil).setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, guid)
        verify(accessState).isNewlyCreated = true
        verify(appUtil).sharedKey = sharedKey
        verify(view).startPinEntryActivity()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `validateCredentials invalid email`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john", "MyTestWallet", "MyTestWallet")
        // Assert
        verify(view).showToast(R.string.invalid_email, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials short password`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john@snow.com", "aaa", "aaa")
        // Assert
        verify(view).showToast(R.string.invalid_password_too_short, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials password missmatch`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john@snow.com", "MyTestWallet", "MyTestWallet2")
        // Assert
        verify(view).showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials weak password`() {
        // Arrange

        // Act
        subject.passwordStrength = 20
        subject.validateCredentials("john@snow.com", "aaaaaa", "aaaaaa")
        // Assert
        verify(view).showWeakPasswordDialog(any(), any())
        verifyNoMoreInteractions(view)
    }
}
