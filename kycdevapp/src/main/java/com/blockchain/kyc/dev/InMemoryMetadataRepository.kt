package com.blockchain.kyc.dev

import com.blockchain.metadata.MetadataRepository
import com.blockchain.serialization.JsonSerializable
import io.reactivex.Completable
import io.reactivex.Maybe
import java.util.concurrent.TimeUnit

internal class InMemoryMetadataRepository : MetadataRepository {

    private val lock = Any()

    private val map = mutableMapOf<Int, Any>()

    override fun <T : JsonSerializable> loadMetadata(metadataType: Int, clazz: Class<T>): Maybe<T> {
        val item = synchronized(lock) {
            map[metadataType]
        }
        return if (item == null) {
            Maybe.empty()
        } else {
            Maybe.just(item).cast(clazz)
        }
    }

    override fun <T : JsonSerializable> saveMetadata(data: T, clazz: Class<T>, metadataType: Int): Completable {
        synchronized(lock) {
            map[metadataType] = data
        }
        return Completable.timer(2, TimeUnit.SECONDS)
    }
}
