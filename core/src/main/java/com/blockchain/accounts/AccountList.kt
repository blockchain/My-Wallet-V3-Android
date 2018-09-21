package com.blockchain.accounts

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency

interface AccountList {
    fun defaultAccountReference(): AccountReference
}

interface AllAccountList {
    operator fun get(cryptoCurrency: CryptoCurrency): AccountList
}
