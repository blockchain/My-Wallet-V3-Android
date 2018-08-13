package com.blockchain.kyc.models.nabu

data class NabuBasicUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val dateOfBirth: String
)