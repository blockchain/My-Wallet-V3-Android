package com.blockchain.kyc

import io.reactivex.Single

class DisabledFeatureFlag(
    override val enabled: Single<Boolean> = Single.just(false)
) : FeatureFlag