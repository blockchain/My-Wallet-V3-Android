package com.blockchain.accounts

import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.junit.Test

class AllAccountListsTest {

    @Test
    fun `can get BTC`() {
        val btcAccountList = mock<AccountList>()
        val allAccountList: AllAccountList = AllAccountsImplementation(
            btcAccountList = btcAccountList,
            bchAccountList = mock(),
            etherAccountList = mock()
        )
        allAccountList[CryptoCurrency.BTC] `should be` btcAccountList
    }

    @Test
    fun `can get BCH`() {
        val bchAccountList = mock<AccountList>()
        val allAccountList: AllAccountList = AllAccountsImplementation(
            btcAccountList = mock(),
            bchAccountList = bchAccountList,
            etherAccountList = mock()
        )
        allAccountList[CryptoCurrency.BCH] `should be` bchAccountList
    }

    @Test
    fun `can get ETH`() {
        val ethAccountList = mock<AccountList>()
        val allAccountList: AllAccountList = AllAccountsImplementation(
            btcAccountList = mock(),
            bchAccountList = mock(),
            etherAccountList = ethAccountList
        )
        allAccountList[CryptoCurrency.ETHER] `should be` ethAccountList
    }
}
