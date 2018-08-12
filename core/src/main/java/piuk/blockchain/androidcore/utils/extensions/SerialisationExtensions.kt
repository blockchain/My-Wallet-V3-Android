@file:JvmName("SerialisationUtils")

package piuk.blockchain.androidcore.utils.extensions

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.Moshi
import org.json.JSONException

/**
 * Converts a deserialized object from a [String] without needing the KClass passed to it.
 *
 * @throws JSONException
 */
@Throws(JSONException::class)
inline fun <reified T> String.toKotlinObject(): T {
    val mapper = ObjectMapper()
    return mapper.readValue(this, T::class.java)
}

/**
 * Serialises any object to a [String] using Jackson.
 *
 * @throws JsonProcessingException
 */
@Throws(JsonProcessingException::class)
fun Any.toSerialisedString(): String {
    val mapper = ObjectMapper()
    return mapper.writeValueAsString(this)
}

/**
 * Serialises any object to a [String] using Moshi.
 */
inline fun <reified T> String.toMoshiKotlinObject(): T {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(T::class.java)
    return jsonAdapter.fromJson(this) ?: throw IllegalStateException("Error parsing JSON")
}

/**
 * Serialises any object to a [String] using Moshi.
 */
inline fun <reified T> T.toMoshiSerialisedString(): String {
    val moshi = Moshi.Builder().build()
    val jsonAdapter = moshi.adapter(T::class.java)
    return jsonAdapter.toJson(this)
}