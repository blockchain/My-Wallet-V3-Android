package com.blockchain.network.websocket

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class MoshiJsonWebSocketDecoratorTest {

    @Suppress("unused")
    class TypeOut(val fieldA: String, val fieldB: Int)

    data class TypeIn(val fieldC: String, val fieldD: Int)

    private val moshi = Moshi.Builder().build()

    @Test
    fun `open delegates to inner open`() {
        val inner = mock<WebSocket<String, String>>()
        inner.toJsonSocket<TypeOut, TypeIn>(moshi)
            .open()
        verify(inner).open()
    }

    @Test
    fun `close delegates to inner close`() {
        val inner = mock<WebSocket<String, String>>()
        inner.toJsonSocket<TypeOut, TypeIn>(moshi)
            .close()
        verify(inner).close()
    }

    @Test
    fun `connection events delegates to inner property`() {
        val events = mock<Observable<ConnectionEvent>>()
        val inner = mock<WebSocket<String, String>> {
            on { connectionEvents } `it returns` events
        }
        inner.toJsonSocket<TypeOut, TypeIn>(moshi)
            .connectionEvents `should be` events
    }

    @Test
    fun `outgoing message is formatted to json`() {
        val inner = mock<WebSocket<String, String>>()
        inner.toJsonSocket<TypeOut, TypeIn>(moshi)
            .send(TypeOut(fieldA = "Message", fieldB = 1234))
        verify(inner).send("{\"fieldA\":\"Message\",\"fieldB\":1234}")
    }

    @Test
    fun `incoming message is formatted from json`() {
        val inner = mock<WebSocket<String, String>> {
            on { responses } `it returns` Observable.just(
                "{\"fieldC\":\"Message1\",\"fieldD\":1234}",
                "{\"fieldC\":\"Message2\",\"fieldD\":5678}"
            )
        }
        inner.toJsonSocket<TypeOut, TypeIn>(moshi)
            .responses
            .test()
            .values() `should equal`
            listOf(
                TypeIn(fieldC = "Message1", fieldD = 1234),
                TypeIn(fieldC = "Message2", fieldD = 5678)
            )
    }
}
