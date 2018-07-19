package info.blockchain.wallet.payload

import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.api.blockexplorer.FilterType
import info.blockchain.api.data.Balance
import info.blockchain.balance.CryptoCurrency
import retrofit2.Call

import java.util.HashMap

class BalanceManagerBch(
    blockExplorer: BlockExplorer
) : BalanceManager(
    blockExplorer,
    CryptoCurrency.BCH
) {

    override fun getBalanceOfAddresses(addresses: List<String>): Call<HashMap<String, Balance>> {
        return blockExplorer.getBalance("bch", addresses, FilterType.RemoveUnspendable)
    }
}
