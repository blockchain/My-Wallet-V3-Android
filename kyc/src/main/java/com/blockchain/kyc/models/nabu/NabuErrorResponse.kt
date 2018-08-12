package com.blockchain.kyc.models.nabu

import android.annotation.SuppressLint
import com.squareup.moshi.Moshi
import retrofit2.Response

private data class NabuErrorResponse(
    /**
     * Machine-readable error type.
     */
    val type: String,
    /**
     * Human-readable error description.
     */
    val description: String
)

class NabuApiException private constructor(message: String) : Throwable(message) {

    private var _httpErrorCode: Int = -1
    private lateinit var _error: String
    private lateinit var _errorDescription: String

    fun getErrorCode(): NabuErrorCodes = NabuErrorCodes.fromErrorCode(_httpErrorCode)

    /**
     * Returns a human-readable error message.
     */
    fun getErrorDescription(): String = _errorDescription

    companion object {

        @SuppressLint("SyntheticAccessor")
        fun fromResponseBody(response: Response<*>): NabuApiException {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(NabuErrorResponse::class.java)
            val coinifyErrorResponse = adapter.fromJson(response.errorBody()!!.string())!!

            val httpErrorCode = response.code()
            val error = coinifyErrorResponse.type
            val errorDescription = coinifyErrorResponse.description

            return NabuApiException("$httpErrorCode: $error - $errorDescription")
                .apply {
                    _httpErrorCode = httpErrorCode
                    _error = error
                    _errorDescription = errorDescription
                }
        }
    }
}