package com.blockchain.lockbox.data.models

import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.Json

internal data class LockboxMetadata(
    val devices: List<Device>
) : JsonSerializable {

    fun hasLockbox(): Boolean = !devices.isEmpty()

    companion object {

        const val MetaDataType = 9
    }
}

internal data class Device(
    @field:Json(name = "device_type") val deviceType: String,
    @field:Json(name = "device_name") val deviceName: String,
    val btc: Btc?,
    val bch: Bch?,
    val eth: Eth?,
    val xlm: Xlm?
) : JsonSerializable

internal data class Bch(
    val accounts: List<BitcoinLikeAccount>
) : JsonSerializable

internal data class Btc(
    val accounts: List<BitcoinLikeAccount>
) : JsonSerializable

internal data class Eth(
    val accounts: List<EthAccount>,
    @field:Json(name = "last_tx") val lastTx: Any,
    @field:Json(name = "last_tx_timestamp") val lastTxTimestamp: Any
) : JsonSerializable

internal data class BitcoinLikeAccount(
    val label: String,
    val archived: Boolean,
    val xpub: String,
    @field:Json(name = "address_labels") val addressLabels: List<Any>,
    val cache: Cache
) : JsonSerializable

internal data class Cache(
    val receiveAccount: String,
    val changeAccount: String
) : JsonSerializable

internal data class EthAccount(
    val label: String,
    val archived: Boolean,
    val correct: Boolean,
    val addr: String
) : JsonSerializable

internal data class Xlm(
    @field: Json(name = "default_account_idx") val defaultAccountIndex: Int,
    val accounts: List<XlmAccount>,
    @field: Json(name = "tx_notes") val txNotes: Map<String, String>?
) : JsonSerializable

internal data class XlmAccount(
    val publicKey: String,
    val label: String,
    val archived: Boolean
) : JsonSerializable