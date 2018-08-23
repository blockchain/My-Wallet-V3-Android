package com.blockchain.serialization

/**
 * Exposes a meta data code which is not serialized.
 */
interface Saveable : JsonSerializable {

    fun getMetadataType(): Int

    fun toJson(): String
}
