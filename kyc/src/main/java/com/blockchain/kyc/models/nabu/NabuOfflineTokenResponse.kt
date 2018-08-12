package com.blockchain.kyc.models.nabu

import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata

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

fun NabuOfflineTokenResponse.mapToMetadata(): NabuCredentialsMetadata =
    NabuCredentialsMetadata(this.userId, this.token)

fun NabuCredentialsMetadata.mapFromMetadata(): NabuOfflineTokenResponse =
    NabuOfflineTokenResponse(this.userId, this.lifetimeToken)