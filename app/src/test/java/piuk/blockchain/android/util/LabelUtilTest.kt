package piuk.blockchain.android.util

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import org.amshove.kluent.shouldEqual
import org.junit.Test
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class LabelUtilTest {

    @Test
    fun `label found in accounts`() {
        // Arrange
        val newLabel = "NEW_LABEL"
        val mockPayloadManager: PayloadDataManager = mock()
        val mockBchDataManager: BchDataManager = mock()
        val account = Account().apply { label = newLabel }
        whenever(mockPayloadManager.accounts).thenReturn(listOf(account))
        whenever(mockBchDataManager.getAccountMetadataList()).thenReturn(emptyList())
        // Act
        val result = LabelUtil.isExistingLabel(mockPayloadManager, mockBchDataManager, newLabel)
        // Assert
        verify(mockPayloadManager).accounts
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual true
    }

    @Test
    fun `label found in accounts despite cases not matching`() {
        // Arrange
        val newLabel = "NEW_LABEL"
        val mockPayloadManager: PayloadDataManager = mock()
        val mockBchDataManager: BchDataManager = mock()
        val account = Account().apply { label = newLabel.toLowerCase() }
        whenever(mockPayloadManager.accounts).thenReturn(listOf(account))
        whenever(mockBchDataManager.getAccountMetadataList()).thenReturn(emptyList())
        // Act
        val result = LabelUtil.isExistingLabel(mockPayloadManager, mockBchDataManager, newLabel)
        // Assert
        verify(mockPayloadManager).accounts
        verifyNoMoreInteractions(mockPayloadManager)
        result shouldEqual true
    }

    @Test
    fun `label found in legacy addresses`() {
        // Arrange
        val newLabel = "NEW_LABEL"
        val mockPayloadManager: PayloadDataManager = mock()
        val mockBchDataManager: BchDataManager = mock()
        val legacyAddress = LegacyAddress().apply { label = newLabel }
        val account = Account()
        whenever(mockPayloadManager.accounts).thenReturn(listOf(account))
        whenever(mockPayloadManager.legacyAddresses).thenReturn(mutableListOf(legacyAddress))
        whenever(mockBchDataManager.getAccountMetadataList()).thenReturn(emptyList())
        // Act
        val result = LabelUtil.isExistingLabel(mockPayloadManager, mockBchDataManager, newLabel)
        // Assert
        verify(mockPayloadManager).accounts
        verify(mockPayloadManager).legacyAddresses
        verifyNoMoreInteractions(mockPayloadManager)
        verify(mockBchDataManager).getAccountMetadataList()
        result shouldEqual true
    }

    @Test
    fun `label found in BCH accounts`() {
        // Arrange
        val newLabel = "NEW_LABEL"
        val mockPayloadManager: PayloadDataManager = mock()
        val mockBchDataManager: BchDataManager = mock()
        val legacyAddress = LegacyAddress().apply { label = newLabel }
        val account = GenericMetadataAccount()
        whenever(mockPayloadManager.accounts).thenReturn(emptyList())
        whenever(mockPayloadManager.legacyAddresses).thenReturn(mutableListOf(legacyAddress))
        whenever(mockBchDataManager.getAccountMetadataList()).thenReturn(listOf(account))
        // Act
        val result = LabelUtil.isExistingLabel(mockPayloadManager, mockBchDataManager, newLabel)
        // Assert
        verify(mockPayloadManager).accounts
        verify(mockPayloadManager).legacyAddresses
        verifyNoMoreInteractions(mockPayloadManager)
        verify(mockBchDataManager).getAccountMetadataList()
        result shouldEqual true
    }

    @Test
    fun `label not found`() {
        // Arrange
        val newLabel = "NEW_LABEL"
        val mockPayloadManager: PayloadDataManager = mock()
        val mockBchDataManager: BchDataManager = mock()
        val legacyAddress0 = LegacyAddress()
        val legacyAddress1 = LegacyAddress().apply { label = "not the label" }
        val account = Account().apply { label = "not the label" }
        whenever(mockPayloadManager.accounts).thenReturn(listOf(account))
        whenever(mockBchDataManager.getAccountMetadataList()).thenReturn(emptyList())
        whenever(mockPayloadManager.legacyAddresses)
            .thenReturn(mutableListOf(legacyAddress0, legacyAddress1))
        // Act
        val result = LabelUtil.isExistingLabel(mockPayloadManager, mockBchDataManager, newLabel)
        // Assert
        verify(mockPayloadManager).accounts
        verify(mockPayloadManager).legacyAddresses
        verifyNoMoreInteractions(mockPayloadManager)
        verify(mockBchDataManager).getAccountMetadataList()
        result shouldEqual false
    }
}