package com.blockchain.morph.homebrew

import com.blockchain.morph.homebrew.json.OutSerialized
import com.blockchain.serialization.JsonSerializable
import com.blockchain.testutils.`should be assignable from`
import org.junit.Test

class JsonSerializationTests {

    @Test
    fun `OutSerialized is serialized`() {
        JsonSerializable::class `should be assignable from` OutSerialized::class
    }

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
}
