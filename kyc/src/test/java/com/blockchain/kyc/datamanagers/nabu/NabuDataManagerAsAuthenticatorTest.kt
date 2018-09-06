package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class NabuDataManagerAsAuthenticatorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    @Test
    fun `the token is fetched and passed to the manager`() {

        val metadataManager = givenMetaDataContainingToken("User", "ABC")

        val nabuDataManager = mock<NabuDataManager>()
        val sut = NabuAuthenticator(metadataManager, nabuDataManager) as Authenticator

        val theFunction = mock<(NabuSessionTokenResponse) -> Single<Int>>()
        sut.authenticate(theFunction)
            .test()

        verify(nabuDataManager).authenticate(
            eq(NabuOfflineTokenResponse("User", "ABC")),
            any<(NabuSessionTokenResponse) -> Single<Int>>()
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `the token is fetched and passed to the manager during the authenticate Single Token overload`() {

        val metadataManager = givenMetaDataContainingToken("User", "ABC")

        val nabuDataManager = mock<NabuDataManager> {
            on { currentToken(NabuOfflineTokenResponse("User", "ABC")) } `it returns` Single.just(
                nabuSessionTokenResponse("User", "ABC")
            )
        }
        val sut = NabuAuthenticator(metadataManager, nabuDataManager) as Authenticator

        sut.authenticate()
            .test()
            .values()[0]
            .apply {
                userId `should equal` "User"
                token `should equal` "ABC"
            }
    }

    private fun nabuSessionTokenResponse(
        userId: String,
        token: String
    ): NabuSessionTokenResponse {
        return NabuSessionTokenResponse(
            id = "",
            userId = userId,
            token = token,
            isActive = true,
            expiresAt = "",
            insertedAt = "",
            updatedAt = ""
        )
    }

    private fun givenMetaDataContainingToken(userId: String, token: String): MetadataManager {
        val offlineToken = NabuCredentialsMetadata(userId, token)
        return mock {
            on { fetchMetadata(NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE) } `it returns`
                Observable.just<Optional<String>>(Optional.of(offlineToken.toMoshiJson()))
        }
    }
}
