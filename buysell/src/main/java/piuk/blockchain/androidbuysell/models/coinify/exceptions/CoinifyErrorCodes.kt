package piuk.blockchain.androidbuysell.models.coinify.exceptions

enum class CoinifyErrorCodes(val code: String) {
    // OAuth 2.0 errors, see [https://tools.ietf.org/html/rfc6749#section-5.2] for more details.
    InvalidRequest("invalid_request"),
    InvalidClient("invalid_client"),
    InvalidGrant("invalid_grant"),
    UnauthorizedClient("unauthorized_client"),
    UnsupportedGrantType("unsupported_grant_type"),

    // SignUp Errors
    /**
     * The provided email address is already associated with an existing trader.
     */
    EmailAddressInUse("email_address_in_use"),
    /**
     * The supplied trustedEmailValidationToken was invalid.
     */
    InvalidTrustedEmailValidationToken("invalid_trusted_email_validation_token"),
    /**
     * An internal error happened during sign up. Please try again later.
     */
    InternalError("internal_error"),

    // Trade Errors
    /**
     * Error interpreting the request (wrong input values for query parameters).
     */
    InvalidArgument("invalid_argument"),

    /**
     * The waiting period after the first trade is not over yet.
     */
    WaitAfterFirstTrade("wait_after_first_trade"),

    // KYC Errors
    /**
     * Error, the identity verification is not in an updatable state. (pending or updateRequested).
     */
    StateNotUpdatable("state_not_updatable"),
    /**
     * Error, access token missing or invalid.
     */
    Unauthenticated("unauthenticated"),
    /**
     * Error, identity verification is not found or doesn't exist for trader.
     */
    IdentityVerificationNotFound("identity_verification_not_found"),

    // Bank Account Errors
    /**
     * Provided account.number is an invalid IBAN. This is only for SEPA countries.
     */
    InvalidIban("invalid_iban"),

    // Generic
    /**
     * Resource not found.
     */
    NotFound("not_found"),
    /**
     * Non-specific error type. Either undocumented or not currently handled. If the type is worth
     * knowing about, add it above and document it.
     */
    Unknown("unknown");

    companion object {

        fun fromErrorCode(code: String): CoinifyErrorCodes {
            for (error in CoinifyErrorCodes.values()) {
                if (code.equals(error.code, ignoreCase = true)) {
                    return error
                }
            }

            return Unknown
        }
    }
}