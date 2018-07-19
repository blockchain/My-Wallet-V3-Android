package info.blockchain.wallet.payload

import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.api.blockexplorer.FilterType
import info.blockchain.api.data.Balance
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.exceptions.ServerConnectionException
import java.util.ArrayList

class BalanceCall(
    private val blockExplorer: BlockExplorer,
    private val cryptoCurrency: CryptoCurrency
) : BalanceQuery {

    override fun getBalancesFor(addressesAndXpubs: Set<String>) =
        getBalanceOfAddresses(ArrayList(addressesAndXpubs))
            .execute()
            .let {
                if (!it.isSuccessful) {
                    throw ServerConnectionException(it.errorBody()?.string() ?: "Unknown, no error body")
                }
                it.body()?.finalBalanceMap()
                    ?: throw Exception("No balances returned")
            }

    private fun getBalanceOfAddresses(addresses: List<String>) =
        blockExplorer.getBalance(cryptoCurrency.symbol.toLowerCase(), addresses, FilterType.RemoveUnspendable)
}

private fun <K> Map<K, Balance>.finalBalanceMap() =
    map { (k, v) -> k to v.finalBalance }.toMap()
