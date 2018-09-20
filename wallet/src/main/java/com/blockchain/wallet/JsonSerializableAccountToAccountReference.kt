package com.blockchain.wallet

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account

fun JsonSerializableAccount.toAccountReference(): AccountReference {
    return when (this) {
        is Account -> AccountReference(CryptoCurrency.BTC, label)
        is GenericMetadataAccount -> AccountReference(CryptoCurrency.BCH, label)
        is EthereumAccount -> AccountReference(CryptoCurrency.ETHER, label)
        else -> throw IllegalArgumentException("Account type not implemented")
    }
}
