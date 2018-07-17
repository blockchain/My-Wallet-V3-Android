@file:JvmName("SerialisationUtils")

package piuk.blockchain.androidcore.utils.extensions

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
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