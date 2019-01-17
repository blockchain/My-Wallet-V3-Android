package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.models.nabu.TiersJson
import io.reactivex.Single

interface TierService {

    fun tiers(): Single<TiersJson>
}
