package com.blockchain.morph.homebrew

import com.blockchain.nabu.Authenticator
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.afterOpen
import com.blockchain.serialization.JsonSerializable
import com.blockchain.serialization.toMoshiJson
import io.reactivex.rxkotlin.subscribeBy

fun WebSocket<String, String>.authenticate(authenticator: Authenticator): WebSocket<String, String> =
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
