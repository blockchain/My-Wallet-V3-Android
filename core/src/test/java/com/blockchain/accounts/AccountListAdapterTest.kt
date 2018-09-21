package com.blockchain.accounts

import com.nhaarman.mockito_kotlin.mock
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.payload.data.Account
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.junit.Test

class AccountListAdapterTest {

    @Test
    fun `BtcAccountListAdapter default`() {
        (BtcAccountListAdapter(mock {
            on { this.defaultAccount } `it returns` Account().apply {
                label = "The default account"
                xpub = "xpub"
            }
        }) as AccountList)
            .defaultAccountReference()
            .label `should be` "The default account"
    }

    @Test
    fun `BchAccountListAdapter default`() {
        (BchAccountListAdapter(mock {
            on { this.getAccountMetadataList() } `it returns` listOf(
                GenericMetadataAccount().apply {
                    label = "The first account"
                },
                GenericMetadataAccount().apply {
                    label = "The default bch account"
                    xpub = "xpub"
                }
            )
            on { this.getDefaultAccountPosition() } `it returns` 1
        }) as AccountList)
            .defaultAccountReference()
            .label `should be` "The default bch account"
    }

    @Test
    fun `EtherAccountListAdapter default`() {
        val ethereumAccount = mock<EthereumAccount> {
            on { label } `it returns` "The default eth account"
            on { address } `it returns` "0x1Address"
        }
        val wallet = mock<EthereumWallet> {
            on { account } `it returns` ethereumAccount
        }
        (EthAccountListAdapter(mock {
            on { getEthWallet() } `it returns` wallet
        }) as AccountList)
            .defaultAccountReference()
            .label `should be` "The default eth account"
    }
}
