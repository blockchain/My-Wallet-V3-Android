package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test

class NabuDataManagerAsAuthenticatorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
    }

    @Test
    fun `the token is fetched and passed to the manager`() {

        val token = givenToken("User", "ABC")

        val nabuDataManager = mock<NabuDataManager>()
        val sut = NabuAuthenticator(token, nabuDataManager) as Authenticator

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

        val token = givenToken("User", "ABC")

        val nabuDataManager = mock<NabuDataManager> {
            on { currentToken(NabuOfflineTokenResponse("User", "ABC")) } `it returns` Single.just(
                nabuSessionTokenResponse("User", "ABC")
            )
        }
        val sut = NabuAuthenticator(token, nabuDataManager) as Authenticator

        sut.authenticate()
            .test()
            .values()[0]
            .apply {
                this.userId `should equal` "User"
                this.token `should equal` "ABC"
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

    private fun givenToken(userId: String, token: String): NabuToken =
        mock {
            on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse(userId, token))
        }
}
