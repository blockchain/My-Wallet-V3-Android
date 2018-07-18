package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsNonArchivedLegacyAddressesExtensionTest {

    private fun legacyAddressWithPrivateKey(address: String, privateKey: String = "PRIVATE_KEY") =
        LegacyAddress().also {
            it.privateKey = privateKey
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().nonArchivedLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
        }.nonArchivedLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1").apply { archive() })
        }.nonArchivedLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            legacyAddressList.add(LegacyAddress().apply {
                address = "Address1"
            })
        }.nonArchivedLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
        }.nonArchivedLegacyAddressStrings() `should equal` setOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY2"))
        }.nonArchivedLegacyAddressStrings() `should equal` setOf("Address1")
    }
}