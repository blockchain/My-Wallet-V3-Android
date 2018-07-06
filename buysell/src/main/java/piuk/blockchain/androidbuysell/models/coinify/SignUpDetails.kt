package piuk.blockchain.androidbuysell.models.coinify

@Suppress("unused")
data class SignUpDetails(
    val email: String,
    val defaultCurrency: String,
    val profile: Profile,
    val partnerId: Int,
    val trustedEmailValidationToken: String
) {

    val generateOfflineToken: Boolean = true

    companion object {

        /**
         * Factory method to return the most simple sign up object possible.
         *
         * @param email The user's verified email address.
         * @param defaultCurrency The user's chosen currency code (ISO 4217).
         * @param countryCode The user's chosen country code (ISO 3166-1 alpha-2).
         * @param partnerId Our partner ID. This is retrieved from wallet-options.
         * @param trustedEmailValidationToken The signed JSON web token returned from the wallet
         * endpoint.
         *
         * @return A basic [SignUpDetails] object for initial registration.
         */
        fun basicSignUp(
            email: String,
            defaultCurrency: String,
            countryCode: String,
            partnerId: Int,
            trustedEmailValidationToken: String
        ): SignUpDetails = SignUpDetails(
            email,
            defaultCurrency,
            Profile(Address(countryCode)),
            partnerId,
            trustedEmailValidationToken
        )
    }
}
