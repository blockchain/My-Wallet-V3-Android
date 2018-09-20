package com.blockchain.morph.homebrew

import com.blockchain.nabu.api.CryptoAndFiat
import com.blockchain.nabu.api.CurrencyRatio
import com.blockchain.nabu.api.QuoteJson
import com.blockchain.nabu.api.Value
import com.blockchain.serialization.JsonSerializable
import com.blockchain.testutils.`should be assignable from`
import org.junit.Test

class JsonSerializationTest {

    @Test
    fun `QuoteMessageJson is serialized`() {
        JsonSerializable::class `should be assignable from` QuoteMessageJson::class
    }

    @Test
    fun `Quote is serialized`() {
        JsonSerializable::class `should be assignable from` QuoteJson::class
    }

    @Test
    fun `CurrencyRatio is serialized`() {
        JsonSerializable::class `should be assignable from` CurrencyRatio::class
    }

    @Test
    fun `CryptoAndFiat is serialized`() {
        JsonSerializable::class `should be assignable from` CryptoAndFiat::class
    }

    @Test
    fun `Value is serialized`() {
        JsonSerializable::class `should be assignable from` Value::class
    }

    @Test
    fun `QuoteWebSocketParams is serialized`() {
        JsonSerializable::class `should be assignable from` QuoteWebSocketParams::class
    }

    @Test
    fun `QuoteWebSocketUnsubscribeParams is serialized`() {
        JsonSerializable::class `should be assignable from` QuoteWebSocketUnsubscribeParams::class
    }
}
