package piuk.blockchain.androidbuysell.models.coinify.exceptions

import android.annotation.SuppressLint
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import retrofit2.Response

private data class CoinifyErrorResponse(
    // Contains a machine-readable error code, e.g. api_key_required
    val error: String,
    // Contains a human-readable error message
    @field:Json(name = "error_description") val errorDescription: String,
    // Contains a URI with more information about the specific error.
    @field:Json(name = "error_uri") val errorUri: String?
)

class CoinifyApiException private constructor(message: String) : Throwable(message) {

    private var _httpErrorCode: Int = -1
    private lateinit var _error: String
    private lateinit var _errorDescription: String
    private var _errorUri: String? = null

    /**
     * Returns a machine-readable error code, e.g. api_key_required.
     */
    fun getErrorCode(): CoinifyErrorCodes = CoinifyErrorCodes.fromErrorCode(_error)

    /**
     * Returns a human-readable error message.
     */
    fun getErrorDescription(): String = _errorDescription

    /**
     * Returns a URI with more information about the specific error.
     */
    fun getErrorUri(): String? = _errorUri

    companion object {

        @SuppressLint("SyntheticAccessor")
        fun fromResponseBody(response: Response<*>): CoinifyApiException {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(CoinifyErrorResponse::class.java)
            val coinifyErrorResponse = adapter.fromJson(response.errorBody()!!.string())!!

            val httpErrorCode = response.code()
            val error = coinifyErrorResponse.error
            val errorDescription = coinifyErrorResponse.errorDescription
            val errorUri = coinifyErrorResponse.errorUri

            return CoinifyApiException("$httpErrorCode: $error - $errorDescription")
                .apply {
                    _httpErrorCode = httpErrorCode
                    _error = error
                    _errorDescription = errorDescription
                    _errorUri = errorUri
                }
        }
    }
}