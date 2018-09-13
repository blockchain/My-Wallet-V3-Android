package com.blockchain.network.websocket

import com.blockchain.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class RetryingConnectionTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val rx = rxInit {
        computation(testScheduler)
    }

    @Test
    fun `passes on open to inner`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        verify(connection.mock).open()
    }

    @Test
    fun `passes on close to inner`() {
        val connection = MockConnection()
        connection.autoRetry().close()
        verify(connection.mock).close()
    }

    @Test
    fun `if the underlying socket closes, we reopen after 1 second delay`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateDisconnect()
        testScheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(1)).open()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(2)).open()
    }

    @Test
    fun `if the underlying socket closes twice, we reopen twice, each after 1 second delay`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateDisconnect()
        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(2)).open()
        connection.simulateDisconnect()
        testScheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(2)).open()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(3)).open()
    }

    @Test
    fun `if we close the connection, and the underlying socket closes, we do not reopen`() {
        val connection = MockConnection()
        connection.autoRetry()
            .also {
                it.open()
                it.close()
            }
        connection.simulateDisconnect()
        verify(connection.mock).open()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        verify(connection.mock).open()
    }

    @Test
    fun `WebSocket autoRetry, if the underlying socket closes, we reopen`() {
        val connection = MockConnection()
        val webSocket: WebSocket<String, String> = mock<WebSocketSendReceive<String, String>>() + connection
        webSocket.autoRetry().open()
        connection.simulateDisconnect()
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        verify(connection.mock, times(2)).open()
    }

    @Test
    fun `multiple disconnect events do not cause multiple connections`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateDisconnect()
        connection.simulateDisconnect()
        connection.simulateDisconnect()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        verify(connection.mock, times(2)).open()
    }

    @Test
    fun `if the connection is opened and closed many times, will still recover`() {
        val connection = MockConnection()
        connection.autoRetry().apply {
            open()
            close()
            open()
            close()
            open()
        }
        connection.simulateDisconnect()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        verify(connection.mock, times(4)).open()
    }
}

class RetryingConnectionFailingConnectionDelayTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val rx = rxInit {
        computation(testScheduler)
    }

    @Test
    fun `if the underlying socket fails, we retry after 1 second delay`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateFailure()
        testScheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(1)).open()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(2)).open()
    }

    @Test
    fun `if the underlying socket fails twice, we retry after a 2 second delay`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateFailure()

        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(2)).open()

        connection.simulateFailure()
        testScheduler.advanceTimeBy(1999, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(2)).open()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(3)).open()
    }

    @Test
    fun `if the underlying socket fails 3 times, we retry after a 4 second delay`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateFailure()
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        connection.simulateFailure()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        verify(connection.mock, times(3)).open()

        connection.simulateFailure()
        testScheduler.advanceTimeBy(3999, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(3)).open()

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(4)).open()
    }

    @Test
    fun `if the underlying socket fails 4 times, we remain on a 4 second delay`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateFailure()
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        connection.simulateFailure()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        connection.simulateFailure()
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS)

        verify(connection.mock, times(4)).open()

        connection.simulateFailure()
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS)

        verify(connection.mock, times(5)).open()
    }

    @Test
    fun `after a successful connection, the timer resets`() {
        val connection = MockConnection()
        connection.autoRetry().open()
        connection.simulateFailure()
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)

        connection.simulateFailure()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)

        connection.simulateFailure()
        testScheduler.advanceTimeBy(4, TimeUnit.SECONDS)
        verify(connection.mock, times(4)).open()

        connection.simulateSuccess()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)

        connection.simulateFailure()
        testScheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(4)).open()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(connection.mock, times(5)).open()
    }
}

class MockConnection(val mock: WebSocketConnection = mock()) : WebSocketConnection by mock {
    private val subject: Subject<ConnectionEvent> = PublishSubject.create<ConnectionEvent>()

    override val connectionEvents: Observable<ConnectionEvent>
        get() = subject

    fun simulateDisconnect() {
        subject.onNext(ConnectionEvent.ClientDisconnect)
    }

    fun simulateFailure() {
        subject.onNext(ConnectionEvent.Failure(Exception()))
    }

    fun simulateSuccess() {
        subject.onNext(ConnectionEvent.Connected)
    }

    fun simulateAuthenticated() {
        subject.onNext(ConnectionEvent.Authenticated)
    }
}
