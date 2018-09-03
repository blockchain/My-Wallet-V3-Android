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
     * Error type not yet specified.
     */
    Unknown(-1);

    companion object {

        fun fromErrorCode(code: Int): NabuErrorCodes =
            NabuErrorCodes.values().firstOrNull { it.code == code } ?: Unknown
    }
}

enum class NabuErrorTypes(val type: String) {
    InternalServerError("INTERNAL_SERVER_ERROR"),
    NotFound("NOT_FOUND"),
    BadMethod("BAD_METHOD"),
    Conflict("CONFLICT"),
    MissingBody("MISSING_BODY"),
    MissingParam("MISSING_PARAM"),
    BadParamValue("BAD_PARAM_VALUE"),
    InvalidCredential("INVALID_CREDENTIALS"),
    WrongPassword("WRONG_PASSWORD"),
    Wrong2FA("WRONG_2FA"),
    Bad2FA("BAD_2FA"),
    UnknownUser("UNKNOWN_USER"),
    InvalidRole("INVALID_ROLE"),
    AlreadyLoggedIn("ALREADY_LOGGED_IN"),
    InvalidStatus("INVALID_STATUS"),
    BadParamValus("BAD_PARAM_VALUE"),
    Unknown("UNKNOWN");

    companion object {

        fun fromErrorStatus(type: String): NabuErrorTypes =
            NabuErrorTypes.values().firstOrNull { it.type == type } ?: Unknown
    }
}