package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

/**
 * An object that represents a recurring buy order for Coinify.
 */
data class Subscription(
    /* Reference to the subscription. */
    val id: Int,
    /* Amount of the subscription. */
    val amount: Double,
    /* Currency of the amount. */
    val currency: String,
    /* If the subscription is active or has been cancelled. */
    val isActive: Boolean,
    /* How often will new trades be created from this subscription. */
    val frequency: BuyFrequency,
    /* The bitcoin address which will receive the recurring payments. */
    val receivingAccount: String,
    /* ISO 8601 date of when the subscription will end, if necessary */
    val endTime: String? = null
)

enum class BuyFrequency(val frequency: String) {
    Daily("daily"),
    Weekly("weekly"),
    Monthly("monthly");

    override fun toString(): String = frequency
}

class BuyFrequencyAdapter {

    @FromJson
    fun fromJson(data: String): BuyFrequency = when (data) {
        "daily" -> BuyFrequency.Daily
        "weekly" -> BuyFrequency.Weekly
        "monthly" -> BuyFrequency.Monthly
        else -> throw JsonDataException("Unknown BuyFrequency $data, unsupported data type")
    }

    @ToJson
    fun toJson(frequency: BuyFrequency) = frequency.toString()
}