package com.blockchain.kyc.models.nabu

import org.amshove.kluent.`should be`
import org.junit.Test

class KycStateCombinerTest {

    @Test
    fun `combinedState when tier 2 is None`() {
        tiers(KycTierState.None, KycTierState.None).combinedState `should be` Kyc2TierState.Locked
        tiers(KycTierState.Pending, KycTierState.None).combinedState `should be` Kyc2TierState.Tier1InReview
        tiers(KycTierState.Verified, KycTierState.None).combinedState `should be` Kyc2TierState.Tier1Approved
        tiers(KycTierState.Rejected, KycTierState.None).combinedState `should be` Kyc2TierState.Tier1Failed
    }

    @Test
    fun `combinedState when tier 2 is Pending`() {
        tiers(KycTierState.None, KycTierState.Pending).combinedState `should be` Kyc2TierState.Tier2InReview
        tiers(KycTierState.Pending, KycTierState.Pending).combinedState `should be` Kyc2TierState.Tier2InReview
        tiers(KycTierState.Verified, KycTierState.Pending).combinedState `should be` Kyc2TierState.Tier2InReview
        tiers(KycTierState.Rejected, KycTierState.Pending).combinedState `should be` Kyc2TierState.Tier2InReview
    }

    @Test
    fun `combinedState when tier 2 is Approved`() {
        tiers(KycTierState.None, KycTierState.Verified).combinedState `should be` Kyc2TierState.Tier2Approved
        tiers(KycTierState.Pending, KycTierState.Verified).combinedState `should be` Kyc2TierState.Tier2Approved
        tiers(KycTierState.Verified, KycTierState.Verified).combinedState `should be` Kyc2TierState.Tier2Approved
        tiers(KycTierState.Rejected, KycTierState.Verified).combinedState `should be` Kyc2TierState.Tier2Approved
    }

    @Test
    fun `combinedState when tier 2 is Rejected`() {
        tiers(KycTierState.None, KycTierState.Rejected).combinedState `should be` Kyc2TierState.Tier2Failed
        tiers(KycTierState.Pending, KycTierState.Rejected).combinedState `should be` Kyc2TierState.Tier2Failed
        tiers(KycTierState.Verified, KycTierState.Rejected).combinedState `should be` Kyc2TierState.Tier2Failed
        tiers(KycTierState.Rejected, KycTierState.Rejected).combinedState `should be` Kyc2TierState.Tier2Failed
    }
}

fun tiers(tier1State: KycTierState, tier2State: KycTierState): TiersJson {
    return TiersJson(
        tiers = listOf(
            TierJson(
                0,
                "Tier 0",
                state = KycTierState.Verified,
                limits = LimitsJson(
                    currency = "USD",
                    daily = null,
                    annual = null
                )
            ),
            TierJson(
                1,
                "Tier 1",
                state = tier1State,
                limits = LimitsJson(
                    currency = "USD",
                    daily = null,
                    annual = 1000.0.toBigDecimal()
                )
            ),
            TierJson(
                2,
                "Tier 2",
                state = tier2State,
                limits = LimitsJson(
                    currency = "USD",
                    daily = 25000.0.toBigDecimal(),
                    annual = null
                )
            )
        )
    )
}