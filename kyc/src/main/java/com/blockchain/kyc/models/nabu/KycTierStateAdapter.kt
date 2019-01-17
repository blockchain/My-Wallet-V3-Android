package com.blockchain.kyc.models.nabu

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import java.util.Locale

internal class KycTierStateAdapter {

    @FromJson
    fun fromJson(input: String): KycTierState =
        when (input.toUpperCase(Locale.US)) {
            NONE -> KycTierState.None
            REJECTED -> KycTierState.Rejected
            PENDING -> KycTierState.Pending
            VERIFIED -> KycTierState.Verified
            else -> throw JsonDataException("Unknown KYC Tier State: $input, unsupported data type")
        }

    @ToJson
    fun toJson(kycTierState: KycTierState): String =
        when (kycTierState) {
            KycTierState.None -> NONE
            KycTierState.Rejected -> REJECTED
            KycTierState.Pending -> PENDING
            KycTierState.Verified -> VERIFIED
        }

    private companion object {
        private const val NONE = "NONE"
        private const val REJECTED = "REJECTED"
        private const val PENDING = "PENDING"
        private const val VERIFIED = "VERIFIED"
    }
}
