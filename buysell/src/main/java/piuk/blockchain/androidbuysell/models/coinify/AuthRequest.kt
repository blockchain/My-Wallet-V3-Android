package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import piuk.blockchain.androidcore.utils.annotations.Mockable

/**
 * An offline token is a lifetime token, granted when a user signs up. It is used
 * to get a temporary access token from the Coinify endpoint.
 */
internal data class AuthRequest(
    @field:Json(name = "grant_type") val grantType: GrantType,
    @field:Json(name = "offline_token") val offlineToken: String
)

enum class GrantType(val type: String) {
    OfflineToken("offline_token"),
    RefreshToken("refresh_token"),
    Password("password");

    override fun toString(): String = type
}

class GrantTypeAdapter {

    @FromJson
    fun fromJson(data: String): GrantType = when (data) {
        "offline_token" -> GrantType.OfflineToken
        "refresh_token" -> GrantType.RefreshToken
        "password" -> GrantType.Password
        else -> throw JsonDataException("Unknown GrantType $data, unsupported data type")
    }

    @ToJson
    fun toJson(grantType: GrantType) = grantType.toString()
}

@Mockable
data class AuthResponse(
    @field:Json(name = "access_token") val accessToken: String,
    @field:Json(name = "token_type") val tokenType: String,
    // Expiry time in seconds, usually 1200
    @field:Json(name = "expires_in") val expiresIn: Int,
    @field:Json(name = "refresh_token") val refreshToken: String? = null
)