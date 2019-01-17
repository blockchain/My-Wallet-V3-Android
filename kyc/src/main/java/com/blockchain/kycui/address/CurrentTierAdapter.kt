package com.blockchain.kycui.address

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.NabuToken
import io.reactivex.Single

internal class CurrentTierAdapter(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : CurrentTier {

    override fun usersCurrentTier(): Single<Int> =
        nabuToken.fetchNabuToken()
            .flatMap(nabuDataManager::getUser)
            .map { it.tiers?.current ?: 0 }
}
