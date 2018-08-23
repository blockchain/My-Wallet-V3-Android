package com.blockchain.nabu.metadata

import com.blockchain.serialization.Saveable
import com.blockchain.serialization.toMoshiJson
import com.squareup.moshi.Json

data class NabuCredentialsMetadata(
    @field:Json(name = "user_id") val userId: String,
    @field:Json(name = "lifetime_token") val lifetimeToken: String
) : Saveable {

    override fun getMetadataType(): Int =
        USER_CREDENTIALS_METADATA_NODE

    override fun toJson(): String = toMoshiJson()

    companion object {
        const val USER_CREDENTIALS_METADATA_NODE = 10
    }
}
