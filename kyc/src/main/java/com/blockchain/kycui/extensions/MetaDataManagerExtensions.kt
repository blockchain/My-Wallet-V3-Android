package com.blockchain.kycui.extensions

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.serialization.fromMoshiJson
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager

internal fun MetadataManager.fetchNabuToken(): Single<NabuOfflineTokenResponse> =
    this.fetchMetadata(NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE)
        .map {
            if (!it.isPresent) throw MetadataNotFoundException("Nabu Token not found")

            NabuCredentialsMetadata::class.fromMoshiJson(it.get())
                .mapFromMetadata()
        }
        .subscribeOn(Schedulers.io())
        .singleOrError()
        .cache()
