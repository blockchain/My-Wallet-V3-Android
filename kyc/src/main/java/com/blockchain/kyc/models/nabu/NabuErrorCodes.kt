package com.blockchain.kyc.models.nabu

enum class NabuErrorCodes(val code: Int) {
    /**
     * The session token has expired, needs reissuing through /auth?userId=userId
     */
    TokenExpired(401),

    /**
     * Conflict. The user's data you're trying to save already exists.
     */
    AlreadyRegistered(409),

    /**
     * Verification code is incorrect.
     */
    VerificationCodeIncorrect(400),

    /**
     * Error type not yet specified.
     */
    Unknown(-1);

    companion object {

        fun fromErrorCode(code: Int): NabuErrorCodes =
            NabuErrorCodes.values().firstOrNull { it.code == code } ?: Unknown
    }
}