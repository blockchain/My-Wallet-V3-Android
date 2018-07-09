package piuk.blockchain.androidbuysell.datamanagers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidbuysell.models.coinify.AuthRequest
import piuk.blockchain.androidbuysell.models.coinify.AuthResponse
import piuk.blockchain.androidbuysell.models.coinify.GrantType
import piuk.blockchain.androidbuysell.models.coinify.Trader
import piuk.blockchain.androidbuysell.models.coinify.exceptions.CoinifyApiException
import piuk.blockchain.androidbuysell.repositories.AccessTokenStore
import piuk.blockchain.androidbuysell.services.CoinifyService
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.utils.Optional
import retrofit2.Response

class CoinifyDataManagerTest : RxTest() {

    private lateinit var subject: CoinifyDataManager
    private val coinifyService: CoinifyService = mock()
    private val authService: AuthService = mock()
    private val accessTokenStore: AccessTokenStore = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        subject = CoinifyDataManager(coinifyService, authService, accessTokenStore)
    }

    @Test
    @Throws(Exception::class)
    fun `get trader token invalid must refresh`() {
        // Arrange
        val offlineToken = "OFFLINE_TOKEN"
        val invalidToken = "INVALID_STORED_TOKEN"
        val invalidStoredResponse: AuthResponse = mock()
        whenever(invalidStoredResponse.accessToken).thenReturn(invalidToken)
        whenever(accessTokenStore.requiresRefresh()).thenReturn(false)
        whenever(accessTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(invalidStoredResponse)))
        // Initial response - unauthenticated
        val responseBody =
            ResponseBody.create(MediaType.parse("application/json"), UNAUTHENTICATED_JSON)
        whenever(coinifyService.getTrader(accessToken = invalidToken))
            .thenReturn(
                Single.error(
                    CoinifyApiException.fromResponseBody(
                        Response.error<Trader>(401, responseBody)
                    )
                )
            )
        // Re-authenticate
        val newlyRefreshedToken = "NEWLY_REFRESHED_TOKEN"
        val newlyRefreshedResponse: AuthResponse = mock()
        whenever(newlyRefreshedResponse.accessToken).thenReturn(newlyRefreshedToken)
        whenever(
            coinifyService.auth(authRequest = AuthRequest(GrantType.OfflineToken, offlineToken))
        ).thenReturn(Single.just(newlyRefreshedResponse))
        // Store new token
        whenever(accessTokenStore.store(newlyRefreshedResponse))
            .thenReturn(Observable.just(newlyRefreshedResponse))
        // Re-attempt getTrader with new token, return successfully
        val trader: Trader = mock()
        whenever(coinifyService.getTrader(accessToken = newlyRefreshedToken))
            .thenReturn(Single.just(trader))
        // Act
        val testObserver = subject.getTrader(offlineToken).test()
        // Assert
        testObserver.assertNoErrors()
        testObserver.assertComplete()
        testObserver.assertValue(trader)
        verify(accessTokenStore).requiresRefresh()
        verify(accessTokenStore).getAccessToken()
        verify(accessTokenStore).invalidate()
        verify(accessTokenStore).store(newlyRefreshedResponse)
        verify(coinifyService).auth(authRequest = AuthRequest(GrantType.OfflineToken, offlineToken))
        verify(coinifyService).getTrader(accessToken = invalidToken)
        verify(coinifyService).getTrader(accessToken = newlyRefreshedToken)
    }

    private companion object {

        private const val UNAUTHENTICATED_JSON = "" +
            "{\n" +
            "  \"error\": \"unauthenticated\",\n" +
            "  \"error_description\": \"Invalid access token\"\n" +
            "}"
    }
}