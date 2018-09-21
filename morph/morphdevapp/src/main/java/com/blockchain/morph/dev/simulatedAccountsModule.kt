package com.blockchain.morph.dev

import com.blockchain.accounts.AccountList
import com.blockchain.accounts.AllAccountList
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import org.koin.dsl.module.applicationContext

val simulatedAccountsModule = applicationContext {

    context("Payload") {

        factory("simulated" + CryptoCurrency.BTC) {
            object : AccountList {
                override fun defaultAccountReference(): AccountReference {
                    return AccountReference.BitcoinLike(
                        CryptoCurrency.BTC, "Simulated Bitcoin account", "xpubNone"
                    )
                }
            } as AccountList
        }

        factory("simulated" + CryptoCurrency.BCH) {
            object : AccountList {
                override fun defaultAccountReference(): AccountReference {
                    return AccountReference.BitcoinLike(
                        CryptoCurrency.BTC, "Simulated BitcoinCash account", "xpubNone"
                    )
                }
            } as AccountList
        }

        factory("simulated" + CryptoCurrency.ETHER) {
            object : AccountList {
                override fun defaultAccountReference(): AccountReference {
                    return AccountReference.Ethereum("Simulated Ethereum account", "0x1Address")
                }
            } as AccountList
        }
        factory {
            object : AllAccountList {
                override fun get(cryptoCurrency: CryptoCurrency): AccountList {
                    return get("simulated$cryptoCurrency")
                }
            } as AllAccountList
        }
    }
}
