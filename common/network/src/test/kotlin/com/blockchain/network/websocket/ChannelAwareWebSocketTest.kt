package com.blockchain.network.websocket

import com.blockchain.serialization.JsonSerializable
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ChannelAwareWebSocketTest {

    @Test
    fun `when open a channel, a subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        underlingSocket.channelAware()
            .openChannel("ChannelName")
        verify(underlingSocket).send(
            "{\"channel\":\"ChannelName\"," +
                "\"operation\":\"subscribe\"" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `when open a channel with parameters, a subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        underlingSocket.channelAware()
            .openChannel("ChannelName", Params(param1 = "ABC"))
        verify(underlingSocket).send(
            "{\"channel\":\"ChannelName\"," +
                "\"operation\":\"subscribe\"," +
                "\"params\":{" +
                "\"param1\":\"ABC\"}" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `channel is filtered`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses } `it returns` Observable.just(
                "{\"channel\":\"ChannelName\"}",
                "{\"x\":\"y\"}",
                "{\"channel\":\"OtherChannel\"}",
                "{\"channel\":\"ChannelName\",\"type\":\"subscribed\"}",
                "{\"channel\":\"ChannelName\",\"type\":\"unsubscribed\"}",
                "null"
            )
        }
        underlingSocket.channelAware()
            .openChannel("ChannelName")
            .responses
            .test()
            .values() `should equal` listOf("{\"channel\":\"ChannelName\"}")
    }

    @Test
    fun `two channels on one webSocket`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses } `it returns` Observable.just(
                "{\"channel\":\"ChannelName\"}",
                "{\"channel\":\"OtherChannel\"}",
                "{\"channel\":\"OtherChannel\",\"message\":\"message2\"}"
            )
        }
        val channelAwareWebSocket = underlingSocket.channelAware()
        channelAwareWebSocket
            .openChannel("ChannelName")
            .responses
            .test()
            .values() `should equal` listOf("{\"channel\":\"ChannelName\"}")
        channelAwareWebSocket
            .openChannel("OtherChannel")
            .responses
            .test()
            .values() `should equal` listOf(
            "{\"channel\":\"OtherChannel\"}",
            "{\"channel\":\"OtherChannel\",\"message\":\"message2\"}"
        )
    }

    @Test
    fun `when close a channel, an subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        val openChannel = underlingSocket.channelAware()
            .openChannel("ChannelName")
        reset(underlingSocket)
        openChannel.close()
        verify(underlingSocket).send(
            "{\"channel\":\"ChannelName\"," +
                "\"operation\":\"unsubscribe\"" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `when close a channel with params, an subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        val openChannel = underlingSocket.channelAware()
            .openChannel("ChannelName")
        reset(underlingSocket)
        openChannel.close(Params(param1 = "closing"))
        verify(underlingSocket).send(
            "{\"channel\":\"ChannelName\"," +
                "\"operation\":\"unsubscribe\"," +
                "\"params\":{" +
                "\"param1\":\"closing\"}" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `when close a channel, the observable completes`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses } `it returns` Observable.never()
        }
        val channelAwareWebSocket = underlingSocket.channelAware()
        val openChannel = channelAwareWebSocket
            .openChannel("ChannelName")
        val test = openChannel
            .responses
            .test()
        test.assertNotComplete()
        openChannel.close()
        test.assertComplete()
    }

    @Test
    fun `errors are not filtered out`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses } `it returns` Observable.just(
                "{\"channel\":\"ChannelName\",\"type\": \"error\"}"
            )
        }
        val channelAwareWebSocket = underlingSocket.channelAware()
        channelAwareWebSocket
            .openChannel("ChannelName")
            .responses
            .test()
            .assertError(ErrorFromServer::class.java)
            .assertErrorMessage("Server returned error")
            .assertError { (it as ErrorFromServer).fullJson == "{\"channel\":\"ChannelName\",\"type\": \"error\"}" }
    }

    class Params(@Suppress("unused") val param1: String) : JsonSerializable
}
