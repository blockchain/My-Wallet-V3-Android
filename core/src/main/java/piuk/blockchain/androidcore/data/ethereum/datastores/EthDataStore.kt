package piuk.blockchain.androidcore.data.ethereum.datastores

import info.blockchain.wallet.ethereum.EthereumWallet
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel

/**
 * A simple data store class to cache the Ethereum Wallet
 */
class EthDataStore : SimpleDataStore {

    var ethWallet: EthereumWallet? = null
    var ethAddressResponse: CombinedEthModel? = null

    override fun clearData() {
        ethWallet = null
        ethAddressResponse = null
    }
}