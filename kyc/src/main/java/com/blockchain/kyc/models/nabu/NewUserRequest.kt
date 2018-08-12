package com.blockchain.kyc.models.nabu

data class NewUserRequest(
    val email: String,
    val walletGuid: String
)

internal data class UserId(val userId: String)