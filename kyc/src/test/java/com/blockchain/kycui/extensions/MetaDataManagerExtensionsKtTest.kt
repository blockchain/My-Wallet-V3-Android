package com.blockchain.kycui.extensions

import com.blockchain.android.testutils.rxInit
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class MetaDataManagerExtensionsKtTest {

    private val metadataManager: MetadataManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Test
    fun `should fetch metadata and map to NabuOfflineTokenResponse`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuCredentialsMetadata(id, lifetimeToken)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        // Act
        val testObserver = metadataManager.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().first()
        userId `should equal to` id
        token `should equal to` lifetimeToken
    }

    @Test
    fun `should throw MetadataNotFoundException`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.absent()))
        // Act
        val testObserver = metadataManager.fetchNabuToken().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(MetadataNotFoundException::class.java)
    }
}