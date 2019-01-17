package com.blockchain.kyc.status

import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.services.nabu.TierService
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith

/**
 * Class contains methods that combine both queries to user and tiers.
 */
class KycTiersQueries(
    private val nabuDataProvider: NabuDataUserProvider,
    private val tiersService: TierService
) {

    fun isKycInProgress(): Single<Boolean> =
        nabuDataProvider
            .getUser()
            .map { it.tiers?.next ?: 0 }
            .zipWith(tiersService.tiers())
            .map { (user, tiers) ->
                tiers.tiers[user].state == KycTierState.None
            }
}
