package com.blockchain.kyc.api.nabu

private const val NABU_APP_PATH: String = "nabu-app/"

internal const val NABU_COUNTRIES = NABU_APP_PATH + "countries"
internal const val NABU_CREATE_USER_ID = NABU_APP_PATH + "internal/users"
internal const val NABU_INITIAL_AUTH = NABU_APP_PATH + "internal/auth"
internal const val NABU_SESSION_TOKEN = NABU_APP_PATH + "auth"
internal const val NABU_USERS_CURRENT = NABU_APP_PATH + "users/current"
internal const val NABU_PUT_ADDRESS = NABU_APP_PATH + "users/current/address"
internal const val NABU_PUT_MOBILE = NABU_APP_PATH + "users/current/mobile"
internal const val NABU_VERIFICATIONS = NABU_APP_PATH + "verifications"
internal const val NABU_ONFIDO_API_KEY = NABU_APP_PATH + "kyc/credentials/onfido"
internal const val NABU_SUBMIT_VERIFICATION = NABU_APP_PATH + "kyc/verifications"