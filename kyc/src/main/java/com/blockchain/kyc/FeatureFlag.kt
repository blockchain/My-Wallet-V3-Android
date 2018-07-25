package com.blockchain.kyc

import io.reactivex.Single

interface FeatureFlag {

    val enabled: Single<Boolean>
}
