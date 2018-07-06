package piuk.blockchain.android.ui.backup.completed

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.androidcore.utils.PrefsUtil

class BackupWalletCompletedPresenterTest {

    private lateinit var subject: BackupWalletCompletedPresenter
    private val view: BackupWalletCompletedView = mock()
    private val transferFundsDataManager: TransferFundsDataManager = mock()
    private val prefsUtil: PrefsUtil = mock()

    @Before
    fun setUp() {
        subject = BackupWalletCompletedPresenter(transferFundsDataManager, prefsUtil)
        subject.initView(view)
    }

    @Test
    fun `onViewReady set backup date`() {
        // Arrange
        val date = 1499181978000L
        whenever(prefsUtil.getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0L))
            .thenReturn(date)
        // Act
        subject.onViewReady()
        // Assert
        verify(prefsUtil).getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0L)
        verifyNoMoreInteractions(prefsUtil)
        verify(view).showLastBackupDate(date)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady hide backup date`() {
        // Arrange
        whenever(prefsUtil.getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0L))
            .thenReturn(0L)
        // Act
        subject.onViewReady()
        // Assert
        verify(prefsUtil).getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0L)
        verifyNoMoreInteractions(prefsUtil)
        verify(view).hideLastBackupDate()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `checkTransferableFunds success`() {
        // Arrange
        val triple =
            org.apache.commons.lang3.tuple.Triple.of(mutableListOf(PendingTransaction()), 0L, 0L)
        whenever(transferFundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.just(triple))
        // Act
        subject.checkTransferableFunds()
        // Assert
        verify(transferFundsDataManager).transferableFundTransactionListForDefaultAccount
        verifyNoMoreInteractions(transferFundsDataManager)
        verify(view).showTransferFundsPrompt()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `checkTransferableFunds failure`() {
        // Arrange
        whenever(transferFundsDataManager.transferableFundTransactionListForDefaultAccount)
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.checkTransferableFunds()
        // Assert
        verify(transferFundsDataManager).transferableFundTransactionListForDefaultAccount
        verifyNoMoreInteractions(transferFundsDataManager)
        verifyZeroInteractions(view)
    }
}
