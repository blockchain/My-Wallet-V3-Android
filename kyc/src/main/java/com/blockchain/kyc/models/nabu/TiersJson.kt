package com.blockchain.kyc.models.nabu

import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

data class TiersJson(
    val tiers: List<TierJson>
) : JsonSerializable {

    val combinedState: Kyc2TierState
        get() {
            val tier2State = tiers[2].state
            return if (tier2State == KycTierState.None) {
                when (tiers[1].state) {
                    KycTierState.None -> Kyc2TierState.Locked
                    KycTierState.Pending -> Kyc2TierState.Tier1InReview
                    KycTierState.Rejected -> Kyc2TierState.Tier1Failed
                    KycTierState.Verified -> Kyc2TierState.Tier1Approved
                }
            } else {
                when (tier2State) {
                    KycTierState.None -> Kyc2TierState.Locked
                    KycTierState.Pending -> Kyc2TierState.Tier2InReview
                    KycTierState.Rejected -> Kyc2TierState.Tier2Failed
                    KycTierState.Verified -> Kyc2TierState.Tier2Approved
                }
            }
        }
}

data class TierJson(
    val index: Int,
    val name: String,
    val state: KycTierState,
    val limits: LimitsJson
) : JsonSerializable

data class LimitsJson(
    val currency: String,
    val daily: BigDecimal?,
    val annual: BigDecimal?
) : JsonSerializable {

    val dailyFiat: FiatValue? get() = daily?.let { FiatValue.fromMajor(currency, it) }

    val annualFiat: FiatValue? get() = annual?.let { FiatValue.fromMajor(currency, it) }
}

enum class KycTierState {
    None,
    Rejected,
    Pending,
    Verified
}

enum class Kyc2TierState {
    Hidden,
    Locked,
    Tier1InReview,
    Tier1Approved,
    Tier1Failed,
    Tier2InReview,
    Tier2Approved,
    Tier2Failed
}
