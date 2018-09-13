package com.blockchain.morph.homebrew

import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.WebSocketConnection
import com.blockchain.network.websocket.plus
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.junit.Test

class AuthWebSocketTest {

    @Test
    fun `after a successful connection, the token from authenticator is sent to the socket`() {
        val connection = MockConnection()
        val underlyingSocket = mock<WebSocket<String, String>>()
        val socket: WebSocket<String, String> =
            (underlyingSocket + connection).authenticate(mock {
                on { authenticate() } `it returns` Single.just(nabuSessionTokenResponse("TheToken"))
            })

        socket.open()
        verifyZeroInteractions(underlyingSocket)
        connection.simulateSuccess()
        verify(underlyingSocket)
            .send(
                "{\"channel\":\"auth\"," +
                    "\"operation\":\"subscribe\"," +
                    "\"params\":{" +
                    "\"token\":\"TheToken\"," +
                    "\"type\":\"auth\"" +
                    "}}"
            )
    }
}

private fun nabuSessionTokenResponse(
    token: String
): NabuSessionTokenResponse {
    return NabuSessionTokenResponse(
        id = "",
        userId = "",
        token = token,
        isActive = true,
        expiresAt = "",
        insertedAt = "",
        updatedAt = ""
    )
}

private class MockConnection(
    val mock: WebSocketConnection = mock()
) : WebSocketConnection by mock {

    private val subject: Subject<ConnectionEvent> = PublishSubject.create<ConnectionEvent>()

    override val connectionEvents: Observable<ConnectionEvent>
        get() = subject

    fun simulateSuccess() {
        subject.onNext(ConnectionEvent.Connected)
    }
}
