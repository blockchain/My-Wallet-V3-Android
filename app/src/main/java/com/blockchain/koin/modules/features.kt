package com.blockchain.koin.modules

import com.blockchain.features.FeatureNames
import piuk.blockchain.android.BuildConfig

val features = mapOf(
    FeatureNames.CONTACTS to BuildConfig.CONTACTS_ENABLED
)
