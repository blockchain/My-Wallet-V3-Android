package info.blockchain.wallet.payload

import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.balance.CryptoCurrency

class BalanceManagerBtc(
    blockExplorer: BlockExplorer
) : BalanceManager(
    blockExplorer,
    CryptoCurrency.BTC
)
