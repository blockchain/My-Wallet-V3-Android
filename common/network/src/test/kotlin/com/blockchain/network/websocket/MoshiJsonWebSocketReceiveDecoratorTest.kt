package com.blockchain.network.websocket

import com.nhaarman.mockito_kotlin.mock
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class MoshiJsonWebSocketReceiveDecoratorTest {

    data class TypeIn(val fieldC: String, val fieldD: Int)

    private val moshi = Moshi.Builder().build()

    @Test
    fun `incoming message is formatted from json`() {
        val inner = mock<WebSocketReceive<String>> {
            on { responses } `it returns` Observable.just(
                "{\"fieldC\":\"Message1\",\"fieldD\":1234}",
                "{\"fieldC\":\"Message2\",\"fieldD\":5678}"
            )
        }
        inner.toJsonReceive<TypeIn>(moshi)
            .responses
            .test()
            .values() `should equal`
            listOf(
                TypeIn(fieldC = "Message1", fieldD = 1234),
                TypeIn(fieldC = "Message2", fieldD = 5678)
            )
    }
}
