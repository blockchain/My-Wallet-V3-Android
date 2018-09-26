package com.blockchain.morph.homebrew

import com.blockchain.nabu.Authenticator
import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.afterOpen
import com.blockchain.serialization.JsonSerializable
import com.blockchain.serialization.fromMoshiJson
import com.blockchain.serialization.toMoshiJson
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject

fun WebSocket<String, String>.authenticate(authenticator: Authenticator): WebSocket<String, String> =
    AuthenticatorWebSocket(
        authenticator,
        afterOpen {
            authenticator.authenticate()
                .subscribeBy {
                    send(
                        AuthSubscribe(
                            channel = "auth",
                            operation = "subscribe",
                            params = Params(
                                type = "auth",
                                token = it.token
                            )
                        ).toMoshiJson()
                    )
                }
        }
    )

private class AuthenticatorWebSocket(
    private val authenticator: Authenticator,
    private val inner: WebSocket<String, String>
) : WebSocket<String, String> by inner {

    private val authConnectionEventsSubject = PublishSubject.create<ConnectionEvent>()

    override val connectionEvents: Observable<ConnectionEvent>
        get() = inner.connectionEvents.mergeWith(authConnectionEventsSubject)

    private val connections = CompositeDisposable()

    override fun open() {
        connections.clear()
        connections += listenForAuthenticatedMessage()
        inner.open()
    }

    private fun listenForAuthenticatedMessage(): Disposable =
        responses.subscribe {
            val authResponse = AuthenticatedResponse::class.fromMoshiJson(it)
            if (authResponse.channel == "auth" && authResponse.type == "authenticated") {
                authConnectionEventsSubject.onNext(ConnectionEvent.Authenticated)
            }
            if (authResponse.channel == "auth" && authResponse.type == "error") {
                println("AUTH ERROR : Invalidating TOKEN and retrying")
                authenticator.invalidateToken()
                authConnectionEventsSubject.onNext(
                    ConnectionEvent.Failure(AuthenticationException(authResponse.description))
                )
            }
        }

    override fun close() {
        connections.clear()
        inner.close()
    }
}

data class AuthenticationException(private val _message: String?) : Exception(_message ?: "No description")

@Suppress("unused")
private class AuthSubscribe(
    val channel: String,
    val operation: String,
    val params: Params
) : JsonSerializable

private class Params(
    val type: String,
    val token: String
) : JsonSerializable

private class AuthenticatedResponse(
    val channel: String,
    val type: String,
    val description: String?
) : JsonSerializable