package com.blockchain.network.websocket

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.amshove.kluent.mock
import org.junit.Test

class BufferUntilAuthenticatedWebSocketTest {

    private val mockConnection = MockConnection()
    private val inner = mock<WebSocket<String, String>>()
    private val webSocket = (inner + mockConnection).bufferUntilAuthenticated(5)

    @Test
    fun `sends immediately if authorised`() {
        webSocket.open()
        mockConnection.simulateAuthenticated()
        webSocket.send("Test")
        verify(inner).send("Test")
    }

    @Test
    fun `does not send if not yet authorised`() {
        webSocket.open()
        webSocket.send("Test")
        verify(inner, never()).send("Test")
    }

    @Test
    fun `sends a single buffered message after authorization`() {
        webSocket.open()
        webSocket.send("Test")
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test")
    }

    @Test
    fun `sends a single buffered message once after authorization`() {
        webSocket.open()
        webSocket.send("Test")
        mockConnection.simulateAuthenticated()
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test")
    }

    @Test
    fun `sends two buffered messages once after authorization`() {
        webSocket.open()
        webSocket.send("Test1")
        webSocket.send("Test2")
        mockConnection.simulateAuthenticated()
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test1")
        verify(inner).send("Test2")
    }

    @Test
    fun `if lose connection, starts buffering again`() {
        webSocket.open()
        mockConnection.simulateAuthenticated()
        mockConnection.simulateDisconnect()
        webSocket.send("Test1")
        webSocket.send("Test2")
        verify(inner, never()).send(any())
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test1")
        verify(inner).send("Test2")
    }

    @Test
    fun `if close connection, starts buffering again`() {
        webSocket.open()
        mockConnection.simulateAuthenticated()
        webSocket.close()
        webSocket.send("Test1")
        verify(inner, never()).send("Test1")
        webSocket.open()
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test1")
    }

    @Test
    fun `if lose connection twice, does not clear the queue`() {
        webSocket.open()
        mockConnection.simulateAuthenticated()
        mockConnection.simulateFailure()
        webSocket.send("Test1")
        mockConnection.simulateFailure()
        webSocket.send("Test2")
        verify(inner, never()).send(any())
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test1")
        verify(inner).send("Test2")
    }

    @Test
    fun `if lose connection after reconnect, does not resend old values`() {
        webSocket.open()
        webSocket.send("Test1")
        mockConnection.simulateAuthenticated()
        mockConnection.simulateFailure()
        webSocket.send("Test2")
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test1")
        verify(inner).send("Test2")
    }

    @Test
    fun `the queue keeps the last limit values`() {
        webSocket.open()
        webSocket.send("Test1")
        webSocket.send("Test2")
        webSocket.send("Test3")
        webSocket.send("Test4")
        webSocket.send("Test5")
        webSocket.send("Test6")
        mockConnection.simulateAuthenticated()
        verify(inner).send("Test2")
        verify(inner).send("Test3")
        verify(inner).send("Test4")
        verify(inner).send("Test5")
        verify(inner).send("Test6")
        verify(inner, never()).send("Test1")
    }
}
