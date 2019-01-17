package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.nabu.models.mapToMetadata
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test

class MetadataRepositoryNabuTokenAdapterTest {

    @Test
    fun `before subscription, does not access the repository`() {
        val metadataRepository: MetadataRepository = mock()
        val createNabuToken: CreateNabuToken = mock()
        MetadataRepositoryNabuTokenAdapter(metadataRepository, createNabuToken).fetchNabuToken()
        verifyZeroInteractions(metadataRepository)
        verifyZeroInteractions(createNabuToken)
    }

    @Test
    fun `can get token from metadata repository`() {
        MetadataRepositoryNabuTokenAdapter(
            givenMetadata(
                Maybe.just(
                    NabuCredentialsMetadata(
                        userId = "User1",
                        lifetimeToken = "TOKEN123"
                    )
                )
            ),
            mock()
        ).fetchNabuToken()
            .test()
            .values()
            .single() `should equal` NabuOfflineTokenResponse(userId = "User1", token = "TOKEN123")
    }

    @Test
    fun `should fetch metadata and map to NabuOfflineTokenResponse`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuCredentialsMetadata(id, lifetimeToken)
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            givenMetadata(
                Maybe.just(offlineToken)
            ),
            mock()
        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().single()
        userId `should equal to` id
        token `should equal to` lifetimeToken
    }

    @Test
    fun `if the metadata is not found, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToMetadata()
        val metadataRepository = givenMetadata(
            Maybe.empty()
        ).expectSave(data)
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock {
                on { createNabuOfflineToken() } `it returns` Single.just(offlineToken)
            }
        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().single()
        userId `should equal to` id
        token `should equal to` lifetimeToken
        verify(metadataRepository).saveMetadata(
            data,
            NabuCredentialsMetadata::class.java,
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
        )
    }

    @Test
    fun `if the metadata is invalid, it creates it and saves it`() {
        // Arrange
        val id = "ID"
        val lifetimeTokenFound = ""
        val lifetimeToken = "LIFETIME_TOKEN"
        val offlineToken = NabuOfflineTokenResponse(id, lifetimeToken)
        val data = offlineToken.mapToMetadata()
        val metadataRepository = givenMetadata(
            Maybe.just(NabuOfflineTokenResponse(id, lifetimeTokenFound).mapToMetadata())
        ).expectSave(data)
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock {
                on { createNabuOfflineToken() } `it returns` Single.just(offlineToken)
            }
        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val (userId, token) = testObserver.values().single()
        userId `should equal to` id
        token `should equal to` lifetimeToken
        verify(metadataRepository).saveMetadata(
            data,
            NabuCredentialsMetadata::class.java,
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
        )
    }

    @Test
    fun `should throw MetadataNotFoundException as token is invalid from create call`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        val metadata = offlineToken.mapFromMetadata()
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            givenMetadata(
                Maybe.empty()
            ).expectSave(offlineToken),
            mock {
                on { createNabuOfflineToken() } `it returns` Single.just(metadata)
            }
        )
        // Act
        val testObserver = nabuToken.fetchNabuToken().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(MetadataNotFoundException::class.java)
    }

    @Test
    fun `if the metadata becomes available later, it is visible`() {
        val metadataRepository = givenMetadata(
            Maybe.empty()
        )
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            givenCantCreate()
        )
        nabuToken.fetchNabuToken().test()
            .assertNotComplete()
            .assertError(Throwable::class.java)
        metadataRepository.givenMetaData(Maybe.just(NabuCredentialsMetadata("USER1", "TOKEN2")))
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should equal` "USER1"
                this.token `should equal` "TOKEN2"
            }
        metadataRepository.verifyJustLoadCalledNTimes(2)
    }

    private fun givenCantCreate(): CreateNabuToken =
        mock {
            on { createNabuOfflineToken() } `it returns` Single.error(Throwable("Can't create"))
        }

    @Test
    fun `if the metadata is available, it does not update, proving cached`() {
        val metadataRepository = givenMetadata(
            Maybe.just(NabuCredentialsMetadata("USER1", "TOKEN1"))
        )
        val nabuToken = MetadataRepositoryNabuTokenAdapter(
            metadataRepository,
            mock()
        )
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should equal` "USER1"
                this.token `should equal` "TOKEN1"
            }
        metadataRepository.givenMetaData(Maybe.just(NabuCredentialsMetadata("USER2", "TOKEN2")))
        nabuToken.fetchNabuToken().test()
            .assertComplete()
            .values()
            .single().apply {
                this.userId `should equal` "USER1"
                this.token `should equal` "TOKEN1"
            }
        metadataRepository.verifyJustLoadCalledNTimes(1)
    }
}

private fun givenMetadata(metaData: Maybe<NabuCredentialsMetadata>): MetadataRepository =
    mock<MetadataRepository>().givenMetaData(metaData)

private fun MetadataRepository.givenMetaData(
    metadata: Maybe<NabuCredentialsMetadata>?
): MetadataRepository {
    whenever(
        loadMetadata(
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
            NabuCredentialsMetadata::class.java
        )
    ).thenReturn(metadata)
    return this
}

private fun MetadataRepository.expectSave(data: NabuCredentialsMetadata): MetadataRepository {
    whenever(
        saveMetadata(
            data,
            NabuCredentialsMetadata::class.java,
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
        )
    ).thenReturn(Completable.complete())
    return this
}

private fun MetadataRepository.verifyJustLoadCalledNTimes(n: Int) {
    verify(this, times(n)).loadMetadata(
        NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
        NabuCredentialsMetadata::class.java
    )
    verifyNoMoreInteractions(this)
}
