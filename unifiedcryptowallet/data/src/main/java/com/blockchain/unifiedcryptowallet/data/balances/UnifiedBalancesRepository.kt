package com.blockchain.unifiedcryptowallet.data.balances

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.CommonResponse
import com.blockchain.api.selfcustody.PubKeyInfo
import com.blockchain.api.selfcustody.SubscriptionInfo
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.outcome.getOrThrow
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalanceNotFoundException
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money

internal class UnifiedBalancesRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val unifiedBalancesSubscribeStore: UnifiedBalancesSubscribeStore,
    private val unifiedBalancesStore: UnifiedBalancesStore,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs,
) : UnifiedBalancesService {
    /**
     * Specify those to get the balance of a specific Wallet.
     */
    override suspend fun balances(wallet: NetworkWallet?): List<NetworkBalance> {
        val networkWallets = networkAccountsService.allNetworks()

        val pubKeys = networkWallets.associateWith {
            it.publicKey()
        }

        subscribe(pubKeys)

        return unifiedBalancesStore.stream(FreshnessStrategy.Cached(false)).firstOutcome()
            .map { response ->
                response.balances.filter {
                    if (wallet == null) true
                    else it.currency == wallet.currency.networkTicker && it.account.index == wallet.index &&
                        it.account.name == wallet.label
                }.mapNotNull {
                    val cc = assetCatalogue.fromNetworkTicker(it.currency)
                    NetworkBalance(
                        currency = cc ?: return@mapNotNull null,
                        balance = it.balance?.amount?.let { amount ->
                            Money.fromMinor(cc, amount)
                        } ?: return@mapNotNull null,
                        unconfirmedBalance = it.pending?.amount?.let { amount ->
                            Money.fromMinor(cc, amount)
                        } ?: return@mapNotNull null,
                        exchangeRate = ExchangeRate(
                            from = cc,
                            to = currencyPrefs.selectedFiatCurrency,
                            rate = it.price
                        )
                    )
                }
            }
            .getOrThrow()
    }

    override suspend fun balanceForWallet(
        wallet: NetworkWallet
    ): NetworkBalance {
        return balances(wallet).firstOrNull() ?: throw UnifiedBalanceNotFoundException(
            currency = wallet.currency.networkTicker,
            name = wallet.label,
            index = wallet.index
        )
    }

    private suspend fun subscribe(networkAccountsPubKeys: Map<NetworkWallet, String>): CommonResponse {

        val subscriptions = networkAccountsPubKeys.keys.map {
            check(networkAccountsPubKeys[it] != null)
            SubscriptionInfo(
                it.currency.networkTicker,
                AccountInfo(
                    index = it.index,
                    name = it.label
                ),
                pubKeys = listOf(
                    PubKeyInfo(
                        pubKey = networkAccountsPubKeys[it]!!,
                        style = it.style,
                        descriptor = it.descriptor
                    )
                )
            )
        }
        return unifiedBalancesSubscribeStore.stream(FreshnessStrategy.Cached(false).withKey(subscriptions))
            .firstOutcome()
            .getOrThrow()
    }
}
