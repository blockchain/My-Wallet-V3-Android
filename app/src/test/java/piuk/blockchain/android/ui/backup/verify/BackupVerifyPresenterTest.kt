package piuk.blockchain.android.ui.backup.verify

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class BackupVerifyPresenterTest {

    private lateinit var subject: BackupVerifyPresenter
    private val view: BackupVerifyView = mock()
    private val payloadDataManager: PayloadDataManager =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val prefsUtil: PrefsUtil = mock()
    private val backupWalletUtil: BackupWalletUtil = mock()

    @Before
    fun setUp() {
        subject = BackupVerifyPresenter(payloadDataManager, prefsUtil, backupWalletUtil)
        subject.initView(view)
    }

    @Test
    fun onViewReady() {
        // Arrange
        val pairOne = 1 to "word_one"
        val pairTwo = 6 to "word_two"
        val pairThree = 7 to "word_three"
        val sequence = listOf(pairOne, pairTwo, pairThree)
        whenever(view.getPageBundle()).thenReturn(null)
        whenever(backupWalletUtil.getConfirmSequence(null)).thenReturn(sequence)
        // Act
        subject.onViewReady()
        // Assert
        verify(view).getPageBundle()
        verify(view).showWordHints(listOf(1, 6, 7))
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getConfirmSequence(null)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun `onVerifyClicked failure`() {
        // Arrange
        val pairOne = 1 to "word_one"
        val pairTwo = 6 to "word_two"
        val pairThree = 7 to "word_three"
        val sequence = listOf(pairOne, pairTwo, pairThree)
        whenever(backupWalletUtil.getConfirmSequence(null)).thenReturn(sequence)
        // Act
        subject.onVerifyClicked(pairOne.second, pairTwo.second, pairTwo.second)
        // Assert
        verify(view).getPageBundle()
        verify(view).showToast(R.string.backup_word_mismatch, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getConfirmSequence(null)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun `onVerifyClicked success`() {
        // Arrange
        val pairOne = 1 to "word_one"
        val pairTwo = 6 to "word_two"
        val pairThree = 7 to "word_three"
        val sequence = listOf(pairOne, pairTwo, pairThree)
        whenever(backupWalletUtil.getConfirmSequence(null)).thenReturn(sequence)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.wallet!!.hdWallets[0]).thenReturn(mock())
        // Act
        subject.onVerifyClicked(pairOne.second, pairTwo.second, pairThree.second)
        // Assert
        verify(backupWalletUtil).getConfirmSequence(null)
        verifyNoMoreInteractions(backupWalletUtil)
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).getPageBundle()
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(view).showCompletedFragment()
        verifyNoMoreInteractions(view)
        verify(prefsUtil).setValue(eq(BackupWalletActivity.BACKUP_DATE_KEY), any<Int>())
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `updateBackupStatus success`() {
        // Arrange
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.wallet!!.hdWallets[0]).thenReturn(mock())
        // Act
        subject.updateBackupStatus()
        // Assert
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(view).showCompletedFragment()
        verifyNoMoreInteractions(view)
        verify(prefsUtil).setValue(eq(BackupWalletActivity.BACKUP_DATE_KEY), any<Int>())
        verifyNoMoreInteractions(prefsUtil)
    }

    @Test
    fun `updateBackupStatus failure`() {
        // Arrange
        whenever(payloadDataManager.syncPayloadWithServer())
            .thenReturn(Completable.error { Throwable() })
        whenever(payloadDataManager.wallet!!.hdWallets[0]).thenReturn(mock())
        // Act
        subject.updateBackupStatus()
        // Assert
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).hideProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(view).showStartingFragment()
        verifyNoMoreInteractions(view)
        verifyZeroInteractions(prefsUtil)
    }
}
