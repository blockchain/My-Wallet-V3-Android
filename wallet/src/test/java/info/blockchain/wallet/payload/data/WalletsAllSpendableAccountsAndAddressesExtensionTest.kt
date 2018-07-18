package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsAllSpendableAccountsAndAddressesExtensionTest {

    private fun legacyAddressWithPrivateKey(address: String) =
        LegacyAddress().also {
            it.privateKey = "PRIVATE_KEY"
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().allSpendableAccountsAndAddresses() `should equal` emptyList()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
        }.allSpendableAccountsAndAddresses() `should equal` listOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1").apply { archive() })
        }.allSpendableAccountsAndAddresses() `should equal` emptyList()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            legacyAddressList.add(LegacyAddress().apply {
                address = "Address1"
            })
        }.allSpendableAccountsAndAddresses() `should equal` emptyList()
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2"))
        }.allSpendableAccountsAndAddresses() `should equal` listOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
        }.allSpendableAccountsAndAddresses() `should equal` listOf("Address1")
    }

    @Test
    fun `one xpub`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1"))
        }.allSpendableAccountsAndAddresses() `should equal` listOf("XPub1")
    }

    @Test
    fun `two xpubs`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub2"))
        }.allSpendableAccountsAndAddresses() `should equal` listOf("XPub1", "XPub2")
    }

    @Test
    fun `repeated xpubs`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub1"))
        }.allSpendableAccountsAndAddresses() `should equal` listOf("XPub1")
    }

    @Test
    fun `two xpubs, two spendable address and two non-spendable`() {
        Wallet().apply {
            hdWallets = listOf(hdWallet("XPub1", "XPub2"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2"))
            legacyAddressList.add(LegacyAddress().also { it.address = "Address3" })
            legacyAddressList.add(legacyAddressWithPrivateKey("Address4").apply { archive() })
        }.allSpendableAccountsAndAddresses() `should equal` listOf("XPub1", "XPub2", "Address1", "Address2")
    }

    private fun hdWallet(vararg xpubs: String) =
        HDWallet().apply {
            accounts = xpubs.map {
                Account().apply {
                    xpub = it
                }
            }
        }
}
