package com.blockchain.wallet

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account

fun JsonSerializableAccount.toAccountReference(): AccountReference =
    when (this) {
        is Account ->
            AccountReference.BitcoinLike(
                CryptoCurrency.BTC,
                label,
                xpub
            )

        is GenericMetadataAccount ->
            AccountReference.BitcoinLike(
                CryptoCurrency.BCH,
                label,
                xpub
            )

        is EthereumAccount ->
            AccountReference.Ethereum(
                label,
                address
            )

        else -> throw IllegalArgumentException("Account type not implemented")
    }
