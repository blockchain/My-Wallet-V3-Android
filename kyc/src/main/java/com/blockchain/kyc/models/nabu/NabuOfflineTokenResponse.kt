package com.blockchain.kyc.models.nabu

data class NabuOfflineTokenResponse(
    val userId: String,
    val token: String
)

data class NabuSessionTokenResponse(
    val id: String,
    val userId: String,
    val token: String,
    val isActive: Boolean,
    val expiresAt: String,
    val insertedAt: String,
    val updatedAt: String
)