package com.blockchain.core.interest.data

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.store.StoreRequest
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable

internal class InterestRepository(
    private val assetCatalogue: AssetCatalogue,
    private val interestStore: InterestStore
) : InterestService {

    private fun getBalancesFlow(request: StoreRequest): Flow<Map<AssetInfo, InterestAccountBalance>> {
        return interestStore.stream(request)
            .mapData { interestBalanceDetailList ->
                interestBalanceDetailList.mapNotNull { interestBalanceDetails ->
                    (assetCatalogue.fromNetworkTicker(interestBalanceDetails.assetTicker) as? AssetInfo)
                        ?.let { assetInfo -> assetInfo to interestBalanceDetails.toInterestBalance(assetInfo) }
                }.toMap()
            }
            .getDataOrThrow()
    }

    override fun getBalances(request: StoreRequest): Observable<Map<AssetInfo, InterestAccountBalance>> {
        return getBalancesFlow(request)
            .asObservable()
            .onErrorReturn { emptyMap() }
    }

    override fun getBalanceFor(asset: AssetInfo, request: StoreRequest): Observable<InterestAccountBalance> {
        return getBalancesFlow(request)
            .asObservable()
            .onErrorReturn { emptyMap() }
            .map { it.getOrDefault(asset, zeroBalance(asset)) }
    }

    override fun getActiveAssets(request: StoreRequest): Flow<Set<AssetInfo>> {
        return getBalancesFlow(request)
            .map { it.keys }
    }
}

private fun InterestBalanceDetails.toInterestBalance(asset: AssetInfo) =
    InterestAccountBalance(
        totalBalance = CryptoValue.fromMinor(asset, totalBalance),
        pendingInterest = CryptoValue.fromMinor(asset, pendingInterest),
        pendingDeposit = CryptoValue.fromMinor(asset, pendingDeposit),
        totalInterest = CryptoValue.fromMinor(asset, totalInterest),
        lockedBalance = CryptoValue.fromMinor(asset, lockedBalance),
        hasTransactions = true
    )

private fun zeroBalance(asset: Currency): InterestAccountBalance =
    InterestAccountBalance(
        totalBalance = Money.zero(asset),
        pendingInterest = Money.zero(asset),
        pendingDeposit = Money.zero(asset),
        totalInterest = Money.zero(asset),
        lockedBalance = Money.zero(asset)
    )
