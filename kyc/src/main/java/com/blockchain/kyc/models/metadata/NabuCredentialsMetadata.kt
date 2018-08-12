package com.blockchain.kyc.models.metadata

import com.squareup.moshi.Json
import info.blockchain.wallet.metadata.Saveable
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject
import piuk.blockchain.androidcore.utils.extensions.toMoshiSerialisedString

data class NabuCredentialsMetadata(
    @field:Json(name = "user_id") val userId: String,
    @field:Json(name = "lifetime_token") val lifetimeToken: String
) : Saveable {

    override fun getMetadataType(): Int = USER_CREDENTIALS_METADATA_NODE

    override fun fromJson(json: String?): Saveable =
        json?.toMoshiKotlinObject() ?: throw IllegalStateException("JSON value is null")

    override fun toJson(): String = this.toMoshiSerialisedString()

    companion object {
        const val USER_CREDENTIALS_METADATA_NODE = 10
    }
}