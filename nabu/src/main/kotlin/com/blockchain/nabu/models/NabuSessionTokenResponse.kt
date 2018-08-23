package com.blockchain.nabu.models

data class NabuSessionTokenResponse(
    val id: String,
    val userId: String,
    val token: String,
    val isActive: Boolean,
    val expiresAt: String,
    val insertedAt: String,
    val updatedAt: String
)
