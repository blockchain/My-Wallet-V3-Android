package piuk.blockchain.android.ui.backup.wordlist

import android.os.Bundle
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.util.BackupWalletUtil

class BackupWalletWordListPresenterTest {

    private lateinit var subject: BackupWalletWordListPresenter
    private val backupWalletUtil: BackupWalletUtil = mock()
    private val view: BackupWalletWordListView = mock()

    @Before
    fun setUp() {
        subject = BackupWalletWordListPresenter(backupWalletUtil)
        subject.initView(view)
    }

    @Test
    fun `onViewReady no mnemonic`() {
        // Arrange
        val password = "PASSWORD"
        val bundle: Bundle = mock()
        whenever(bundle.getString(ARGUMENT_SECOND_PASSWORD)).thenReturn(password)
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(null)
        // Act
        subject.onViewReady()
        // Assert
        verify(view).getPageBundle()
        verify(view).finish()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun `onViewReady mnemonic loaded`() {
        // Arrange
        val password = "PASSWORD"
        val bundle: Bundle = mock()
        whenever(bundle.getString(ARGUMENT_SECOND_PASSWORD)).thenReturn(password)
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(mnemonic)
        // Act
        subject.onViewReady()
        // Assert
        verify(view).getPageBundle()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
    }

    @Test
    fun getWordForIndex() {
        // Arrange
        val password = "PASSWORD"
        val bundle: Bundle = mock()
        whenever(bundle.getString(ARGUMENT_SECOND_PASSWORD)).thenReturn(password)
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(mnemonic)
        // Act
        subject.onViewReady()
        val result = subject.getWordForIndex(2)
        // Assert
        verify(view).getPageBundle()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
        result `should equal to` "three"
    }

    @Test
    fun getMnemonicSize() {
        // Arrange
        val password = "PASSWORD"
        val bundle: Bundle = mock()
        whenever(bundle.getString(ARGUMENT_SECOND_PASSWORD)).thenReturn(password)
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(view.getPageBundle()).thenReturn(bundle)
        whenever(backupWalletUtil.getMnemonic(password)).thenReturn(mnemonic)
        // Act
        subject.onViewReady()
        val result = subject.getMnemonicSize()
        // Assert
        verify(view).getPageBundle()
        verifyNoMoreInteractions(view)
        verify(backupWalletUtil).getMnemonic(password)
        verifyNoMoreInteractions(backupWalletUtil)
        result `should equal to` 4
    }
}
