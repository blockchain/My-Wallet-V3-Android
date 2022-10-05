package com.blockchain.core.staking.data.datasources

import com.blockchain.api.staking.StakingApiService
import com.blockchain.api.staking.data.StakingBalanceDto
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class StakingBalanceStore(
    private val stakingApiService: StakingApiService,
) : Store<Map<String, StakingBalanceDto>> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.Keyed.ofOutcome(
            mapper = {
                stakingApiService.getStakingBalances()
            }
        ),
        dataSerializer = MapSerializer(
            keySerializer = String.serializer(),
            valueSerializer = StakingBalanceDto.serializer()
        ),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "StakingBalanceStore"
    }
}
