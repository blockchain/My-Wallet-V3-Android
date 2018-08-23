package com.blockchain.serialization

import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.io.Serializable

class SavableTest {

    @Test
    fun `a savable using moshi doesn't write the metadata value`() {
        ASavable("Hello").toJson() `should equal` "{\"aValue\":\"Hello\"}"
    }

    @Test
    fun `a savable with type erased still works`() {
        val saveable: Saveable = ASavable("ABC")
        saveable.toJson() `should equal` "{\"aValue\":\"ABC\"}"
    }

    @Test
    fun `a savable is an instance of Serializable to help with proguard`() {
        object : Saveable {
            override fun getMetadataType() = 0
            override fun toJson() = "{}"
        } `should be instance of` (Serializable::class.java)
    }

    private class ASavable(
        @Suppress("unused") val aValue: String
    ) : Saveable {
        override fun toJson() = toMoshiJson()

        override fun getMetadataType() = 123
    }
}
