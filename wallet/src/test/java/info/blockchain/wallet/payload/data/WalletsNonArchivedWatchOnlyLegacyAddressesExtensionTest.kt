package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsNonArchivedWatchOnlyLegacyAddressesExtensionTest {

    private fun legacyAddressWithPrivateKey(address: String, privateKey: String = "PRIVATE_KEY") =
        LegacyAddress().also {
            it.privateKey = privateKey
            it.address = address
        }

    private fun legacyAddressWithoutPrivateKey(address: String) =
        LegacyAddress().also {
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().nonArchivedWatchOnlyLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1").apply { archive() })
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one watch only and one archived`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2").apply { archive() })
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `two watch only`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address2"))
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` setOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
        }.nonArchivedWatchOnlyLegacyAddressStrings() `should equal` setOf("Address1")
    }
}