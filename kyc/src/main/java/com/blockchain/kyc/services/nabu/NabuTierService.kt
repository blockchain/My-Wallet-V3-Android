package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.extensions.wrapErrorMessage
import com.blockchain.kyc.models.nabu.TierUpdateJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.nabu.Authenticator
import io.reactivex.Completable
import io.reactivex.Single

internal class NabuTierService(
    private val endpoint: Nabu,
    private val authenticator: Authenticator
) : TierService, TierUpdater {

    override fun tiers(): Single<TiersJson> =
        authenticator.authenticate {
            endpoint.getTiers(it.authHeader)
        }.wrapErrorMessage()

    override fun setUserTier(tier: Int): Completable =
        authenticator.authenticate {
            endpoint.setTier(
                TierUpdateJson(tier),
                it.authHeader
            ).toSingleDefault(tier)
        }.ignoreElement()
}
