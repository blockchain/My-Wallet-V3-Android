package piuk.blockchain.androidcore.data.ethereum

import info.blockchain.wallet.ethereum.EthereumAccount
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.androidcore.utils.annotations.Mockable
import javax.inject.Inject

/**
 * This class is simply for making [EthereumAccount.deriveECKey] mockable for testing.
 */
@Mockable
class EthereumAccountWrapper @Inject constructor() {

    fun deriveECKey(masterKey: DeterministicKey, accountIndex: Int): ECKey =
        EthereumAccount.deriveECKey(masterKey, accountIndex)
}