package piuk.blockchain.android.util

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.payload.data.HDWallet
import info.blockchain.wallet.payload.data.Wallet
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class BackupWalletUtilTest {

    private lateinit var subject: BackupWalletUtil
    private val payloadDataManager: PayloadDataManager = mock()

    @Before
    fun setUp() {
        subject = BackupWalletUtil(payloadDataManager)
    }

    @Test
    fun getConfirmSequence() {
        val wallet: Wallet = mock()
        val hdWallet: HDWallet = mock()
        val hdWallets = listOf(hdWallet)
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(wallet.hdWallets).thenReturn(hdWallets)
        whenever(hdWallet.mnemonic).thenReturn(mnemonic)
        // Act
        val result = subject.getConfirmSequence(null)
        // Assert
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        result.size `should equal` 3
    }

    @Test
    fun `getMnemonic success`() {
        // Arrange
        val wallet: Wallet = mock()
        val hdWallet: HDWallet = mock()
        val hdWallets = listOf(hdWallet)
        val mnemonic = listOf("one", "two", "three", "four")
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(wallet.hdWallets).thenReturn(hdWallets)
        whenever(hdWallet.mnemonic).thenReturn(mnemonic)
        // Act
        val result = subject.getMnemonic(null)
        // Assert
        verify(payloadDataManager, times(2)).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(wallet).decryptHDWallet(0, null)
        verify(wallet).hdWallets
        verifyNoMoreInteractions(wallet)
        result `should equal` mnemonic
    }

    @Test
    fun `getMnemonic error`() {
        // Arrange
        val wallet: Wallet = mock()
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        whenever(wallet.decryptHDWallet(0, null)).thenThrow(NullPointerException())
        // Act
        val result = subject.getMnemonic(null)
        // Assert
        verify(payloadDataManager).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(wallet).decryptHDWallet(0, null)
        verifyNoMoreInteractions(wallet)
        result `should equal` null
    }
}