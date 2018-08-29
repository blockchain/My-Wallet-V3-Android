package com.blockchain.kyc.datamanagers.nabu

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
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class NabuDataManagerAsAuthenticatorTest {

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

    private fun givenMetaDataContainingToken(userId: String, token: String): MetadataManager {
        val offlineToken = NabuCredentialsMetadata(userId, token)
        return mock {
            on { fetchMetadata(NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE) } `it returns`
                Observable.just<Optional<String>>(Optional.of(offlineToken.toMoshiJson()))
        }
    }
}
