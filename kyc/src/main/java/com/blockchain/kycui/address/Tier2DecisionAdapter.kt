package com.blockchain.kycui.address

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import io.reactivex.Single

internal class Tier2DecisionAdapter(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : Tier2Decision {

    override fun progressToTier2(): Single<Tier2Decision.NextStep> =
        nabuToken.fetchNabuToken()
            .flatMap(nabuDataManager::getUser)
            .map { user ->
                if (user.tierInProgressOrCurrentTier == 1) {
                    Tier2Decision.NextStep.Tier1Complete
                } else {
                    if (user.tiers == null || user.tiers.next ?: 0 > user.tiers.selected ?: 0) {
                        // the backend is telling us the user should be put down path for tier2 even though they
                        // selected tier 1, so we need to inform them
                        Tier2Decision.NextStep.Tier2ContinueTier1NeedsMoreInfo
                    } else {
                        Tier2Decision.NextStep.Tier2Continue
                    }
                }
            }
}
