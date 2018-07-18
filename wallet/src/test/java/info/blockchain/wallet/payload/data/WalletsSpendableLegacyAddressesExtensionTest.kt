package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should equal`
import org.junit.Test

class WalletsSpendableLegacyAddressesExtensionTest {

    private fun legacyAddressWithPrivateKey(address: String, privateKey: String = "PRIVATE_KEY") =
        LegacyAddress().also {
            it.privateKey = privateKey
            it.address = address
        }

    @Test
    fun `empty list`() {
        Wallet().spendableLegacyAddressStrings() `should equal` emptyList()
    }

    @Test
    fun `one spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1"))
        }.spendableLegacyAddressStrings() `should equal` listOf("Address1")
    }

    @Test
    fun `one archived`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1").apply { archive() })
        }.spendableLegacyAddressStrings() `should equal` emptyList()
    }

    @Test
    fun `one without private key`() {
        Wallet().apply {
            legacyAddressList.add(LegacyAddress().apply {
                address = "Address1"
            })
        }.spendableLegacyAddressStrings() `should equal` emptyList()
    }

    @Test
    fun `two spendable`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address2", "PRIVATE_KEY2"))
        }.spendableLegacyAddressStrings() `should equal` listOf("Address1", "Address2")
    }

    @Test
    fun `repeated address`() {
        Wallet().apply {
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY1"))
            legacyAddressList.add(legacyAddressWithPrivateKey("Address1", "PRIVATE_KEY2"))
        }.spendableLegacyAddressStrings() `should equal` listOf("Address1")
    }
}