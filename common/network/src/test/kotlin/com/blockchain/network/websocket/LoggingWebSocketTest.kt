package com.blockchain.network.websocket

import com.blockchain.logging.Logger
import com.blockchain.logging.NullLogger
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext
import org.koin.test.AutoCloseKoinTest

class LoggingWebSocketTest : AutoCloseKoinTest() {

    private val logger = mock<Logger>()

    @Before
    fun setup() {
        StandAloneContext.startKoin(
            listOf(
                applicationContext {
                    bean {
                        logger
                    }
                }
            )
        )
    }

    @Test
    fun `logs sends`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        val socket = underlyingSocket
            .debugLog("X")

        socket.send("Hello")

        verify(logger).v("WebSocket X send Hello")
        verifyNoMoreInteractions(logger)
        verify(underlyingSocket).send("Hello")
    }

    @Test
    fun `logs receives`() {
        mock<WebSocket<String, String>> {
            on { responses } `it returns` Observable.just("A response")
        }
            .debugLog("Y")
            .responses
            .test()

        verify(logger).v("WebSocket Y receive A response")
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `logs connection events`() {
        mock<WebSocket<String, String>> {
            on { connectionEvents } `it returns` Observable.just<ConnectionEvent>(ConnectionEvent.Connected)
        }
            .debugLog("Z")
            .connectionEvents
            .test()

        verify(logger).d("WebSocket Z Connected")
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `open calls`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket
            .debugLog("Z")
            .open()

        verify(logger).d("WebSocket Z Open called")
        verifyNoMoreInteractions(logger)
        verify(underlyingSocket).open()
    }

    @Test
    fun `close calls`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket
            .debugLog("Z")
            .close()

        verify(logger).d("WebSocket Z Close called")
        verifyNoMoreInteractions(logger)
        verify(underlyingSocket).close()
    }
}

class LoggingWebSocketWithNullLoggerTest : AutoCloseKoinTest() {

    @Before
    fun setup() {
        StandAloneContext.startKoin(
            listOf(
                applicationContext {
                    bean {
                        NullLogger as Logger
                    }
                }
            )
        )
    }

    @Test
    fun `no wrapper socket is provided`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket.debugLog("X") `should be` underlyingSocket
    }
}
