package com.blockchain.kycui.extensions

import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.mapFromMetadata
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiKotlinObject

internal fun MetadataManager.fetchNabuToken(): Single<NabuOfflineTokenResponse> =
    this.fetchMetadata(NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE)
        .map {
            it.get()
                .toMoshiKotlinObject<NabuCredentialsMetadata>()
                .mapFromMetadata()
        }
        .subscribeOn(Schedulers.io())
        .singleOrError()
        .cache()