package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.Json

data class TraderResponse(
    val trader: Trader,
    val offlineToken: String
)

data class Trader(
    val id: Int,
    val defaultCurrency: String,
    val email: String,
    val profile: Profile,
    val level: Level? = null
)

data class Profile(
    val address: Address,
    val name: String? = null,
    val mobile: Mobile? = null
)

data class Address(
    @field:Json(name = "country") val countryCode: String,
    val street: String? = null,
    val zipcode: String? = null,
    val city: String? = null,
    val state: String? = null
) {
    fun getFormattedAddressString(): String {
        val formattedStreet = if (street != null) "$street, " else ""
        val formattedCity = if (city != null) "$city, " else ""
        val formattedZipCode = if (zipcode != null) "$zipcode, " else ""
        val formattedState = if (state != null) "$state, " else ""
        return "$formattedStreet$formattedCity$formattedZipCode$formattedState$countryCode"
    }
}

data class Mobile(val countryCode: String, val number: String)

data class Level(
    val id: Int,
    val name: String,
    val currency: String,
    val feePercentage: Double,
    val limits: Limits? = null
)

data class Limits(val card: Card)

data class Card(@field:Json(name = "in") val inX: In)

/**
 * These values are the max limits denominated in the user's default currency
 * and must therefore be converted if another currency is chosen.
 */
data class In(val daily: Double, val yearly: Double)