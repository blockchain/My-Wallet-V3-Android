package com.blockchain.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.CreateNabuToken
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.nabu.models.mapToMetadata
import com.blockchain.rx.maybeCache
import io.reactivex.Maybe
import io.reactivex.Single

internal class MetadataRepositoryNabuTokenAdapter(
    private val metadataRepository: MetadataRepository,
    private val createNabuToken: CreateNabuToken
) : NabuToken {

    private val createMetaData = Maybe.defer {
        createNabuToken.createNabuOfflineToken()
            .map {
                it.mapToMetadata()
            }
            .flatMapMaybe {
                metadataRepository.saveMetadata(
                    it,
                    NabuCredentialsMetadata::class.java,
                    NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                ).andThen(Maybe.just(it))
            }
    }

    private val defer = Maybe.defer {
        metadataRepository.loadMetadata(
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
            NabuCredentialsMetadata::class.java
        )
    }.maybeCache()
        .filter { it.isValid() }
        .switchIfEmpty(createMetaData)
        .map { metadata ->
            if (!metadata.isValid()) throw MetadataNotFoundException("Nabu Token is empty")
            metadata.mapFromMetadata()
        }
        .toSingle()

    override fun fetchNabuToken(): Single<NabuOfflineTokenResponse> = defer
}
