package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsNonSpendableLegacyAddressesExtensionTest {

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
        Wallet().nonSpendableLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
        }.nonSpendableLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1").apply { archive() })
        }.nonSpendableLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
        }.nonSpendableLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
        }.nonSpendableLegacyAddressStrings() `should equal` emptySet()
    }

    @Test
    fun `two non spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1").apply { archive() })
        }.nonSpendableLegacyAddressStrings() `should equal` setOf("Address1")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
            legacyAddressList.add(legacyAddressWithoutPrivateKey("Address1"))
        }.nonSpendableLegacyAddressStrings() `should equal` setOf("Address1")
    }
}