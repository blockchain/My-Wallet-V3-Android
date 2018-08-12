package com.blockchain.kyc.models.nabu

enum class NabuErrorCodes(val code: Int) {
    /**
     * The session token has expired, needs reissuing through /auth?userId=userId
     */
    TokenExpired(401),

    /**
     * The user's email address is already registered on Nabu.
     */
    EmailAlreadyRegistered(409),

    /**
     * Error type not yet specified.
     */
    Unknown(-1);

    companion object {

        fun fromErrorCode(code: Int): NabuErrorCodes {
            for (error in NabuErrorCodes.values()) {
                if (code == error.code) {
                    return error
                }
            }

            return Unknown
        }
    }
}