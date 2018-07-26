package piuk.blockchain.androidbuysell

import android.support.annotation.CallSuper
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

@Deprecated("Use mockWebServerInit")
abstract class MockWebServerTest {

    protected lateinit var server: MockWebServer

    @Before
    @CallSuper
    open fun setUp() {
        server = MockWebServer()
    }

    @After
    @CallSuper
    open fun tearDown() {
        server.shutdown()
    }
}