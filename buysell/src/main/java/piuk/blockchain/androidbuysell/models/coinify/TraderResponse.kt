package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.Json
import piuk.blockchain.androidcore.utils.annotations.Mockable

@Mockable
data class TraderResponse(
    val trader: Trader,
    val offlineToken: String
)

@Mockable
data class Trader(
    val id: Int,
    val defaultCurrency: String,
    val email: String,
    val profile: Profile,
    val level: Level
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
    val limits: Limits
)

data class Limits(val card: CardLimits, val bank: BankLimits)

data class CardLimits(@field:Json(name = "in") val inLimits: LimitValues)

data class BankLimits(
    @field:Json(name = "in") val inLimits: LimitValues,
    @field:Json(name = "out") val outLimits: LimitValues
)

/**
 * These values are the max limits denominated in the user's default currency
 * and must therefore be converted if another currency is chosen.
 */
data class LimitValues(val daily: Double, val yearly: Double?)