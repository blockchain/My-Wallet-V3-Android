package piuk.blockchain.androidcore.data.bitcoincash

import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataWallet
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple data store class to cache the Bitcoin cash Wallet (bitcoin chain M/44H/0H)
 */
@Singleton
class BchDataStore @Inject constructor() : SimpleDataStore {

    var bchWallet: BitcoinCashWallet? = null
    var bchMetadata: GenericMetadataWallet? = null

    override fun clearData() {
        bchWallet = null
        bchMetadata = null
    }
}