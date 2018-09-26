package com.blockchain.network.websocket

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ChannelFilterTest {

    @Test
    fun `channel is filtered`() {
        mock<WebSocketReceive<String>> {
            on { responses } `it returns` Observable.just(
                "{\"channel\":\"ChannelName\"}",
                "{\"x\":\"y\"}",
                "{\"channel\":\"OtherChannel\"}",
                "{\"channel\":\"ChannelName\",\"type\":\"subscribed\"}",
                "{\"channel\":\"ChannelName\",\"type\":\"unsubscribed\"}",
                "null"
            )
        }.channelMessageFilter("ChannelName")
            .responses
            .test()
            .values() `should equal` listOf("{\"channel\":\"ChannelName\"}")
    }

    @Test
    fun `errors can be ignored`() {
        mock<WebSocketReceive<String>> {
            on { responses } `it returns` Observable.just(
                "{\"channel\":\"ChannelName\",\"type\":\"error\"}",
                "{\"channel\":\"ChannelName\"}"
            )
        }.channelMessageFilter("ChannelName", throwErrors = false)
            .responses
            .test()
            .apply {
                assertNoErrors()
                values() `should equal` listOf("{\"channel\":\"ChannelName\"}")
            }
    }

    @Test
    fun `errors can be thrown - by default`() {
        mock<WebSocketReceive<String>> {
            on { responses } `it returns` Observable.just(
                "{\"channel\":\"ChannelName\",\"type\":\"error\"}",
                "{\"channel\":\"ChannelName\"}"
            )
        }.channelMessageFilter("ChannelName")
            .responses
            .test()
            .apply {
                assertError {
                    it `should be instance of` ErrorFromServer::class
                    it.message `should equal` "Server returned error"
                    (it as ErrorFromServer).fullJson `should equal` "{\"channel\":\"ChannelName\",\"type\":\"error\"}"
                    true
                }
            }
    }
}
