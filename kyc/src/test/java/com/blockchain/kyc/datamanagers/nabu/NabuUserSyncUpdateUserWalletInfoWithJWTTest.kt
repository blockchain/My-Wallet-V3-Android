package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Rule
import org.junit.Test

class NabuUserSyncUpdateUserWalletInfoWithJWTTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `no interactions until subscribe`() {
        val nabuToken = mock<NabuToken>()
        val nabuDataManager = mock<NabuDataManager>()
        val nabuUserSync = givenSyncInstance(nabuDataManager, nabuToken)
        nabuUserSync
            .syncUser()
        verifyZeroInteractions(nabuToken)
        verifyZeroInteractions(nabuDataManager)
    }

    @Test
    fun `on sync user`() {
        val jwt = "JWT"
        val offlineToken = NabuOfflineTokenResponse("", "")
        val nabuToken: NabuToken = mock {
            on { fetchNabuToken() } `it returns` Single.just(offlineToken)
        }
        val nabuDataManager: NabuDataManager = mock {
            on { requestJwt() } `it returns` Single.just(jwt)
            on { updateUserWalletInfo(offlineToken, jwt) } `it returns` Single.just(getBlankNabuUser())
        }

        val nabuUserSync = givenSyncInstance(nabuDataManager, nabuToken)

        nabuUserSync
            .syncUser()
            .test()
            .assertComplete()

        verify(nabuToken).fetchNabuToken()
        verifyNoMoreInteractions(nabuToken)

        verify(nabuDataManager).updateUserWalletInfo(offlineToken, jwt)
        verify(nabuDataManager).requestJwt()
        verifyNoMoreInteractions(nabuDataManager)
    }

    private fun givenSyncInstance(
        nabuDataManager: NabuDataManager,
        nabuToken: NabuToken
    ): NabuUserSync =
        NabuUserSyncUpdateUserWalletInfoWithJWT(nabuDataManager, nabuToken)
}
