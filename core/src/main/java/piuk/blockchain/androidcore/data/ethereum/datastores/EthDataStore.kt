package piuk.blockchain.androidcore.data.ethereum.datastores

import info.blockchain.wallet.ethereum.EthereumWallet
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple data store class to cache the Ethereum Wallet
 */
@Singleton
class EthDataStore @Inject constructor() : SimpleDataStore {

    var ethWallet: EthereumWallet? = null
    var ethAddressResponse: CombinedEthModel? = null

    override fun clearData() {
        ethWallet = null
        ethAddressResponse = null
    }
}