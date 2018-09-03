package com.blockchain.serialization

import com.squareup.moshi.Moshi
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * Useful for limiting the objects/classes IDE suggests serialization on as well as using in proguard
 */
interface JsonSerializable : Serializable

/**
 * Deserialize any [JsonSerializable] from a [String] using Moshi.
 */
fun <T : JsonSerializable> KClass<T>.fromMoshiJson(json: String): T {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(this.java)
    return jsonAdapter.fromJson(json) ?: throw IllegalStateException("Error parsing JSON")
}

/**
 * Serialize any [JsonSerializable] to a [String] using Moshi.
 */
inline fun <reified T : JsonSerializable> T.toMoshiJson() = toMoshiJson(T::class.java)

inline fun <reified T : JsonSerializable> T.toMoshiJson(clazz: Class<T>): String {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(clazz)
    return jsonAdapter.toJson(this)
}
