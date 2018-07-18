package info.blockchain.wallet.payload.data

import org.amshove.kluent.`should be`
import org.junit.Test

class LegacyAddressExtensionsTest {

    @Test
    fun `is not archived initially`() {
        LegacyAddress().isArchived `should be` false
    }

    @Test
    fun `tag is not set initially`() {
        LegacyAddress().tag `should be` 0
    }

    @Test
    fun `archive marks address as archived`() {
        LegacyAddress().apply {
            archive()
            isArchived `should be` true
        }
    }

    @Test
    fun `tag is set by archive`() {
        LegacyAddress().apply {
            archive()
            tag `should be` LegacyAddress.ARCHIVED_ADDRESS
        }
    }

    @Test
    fun `tag set marks address as archived`() {
        LegacyAddress().apply {
            tag = LegacyAddress.ARCHIVED_ADDRESS
            isArchived `should be` true
        }
    }

    @Test
    fun `tag set to normal, clears archived`() {
        LegacyAddress().apply {
            tag = LegacyAddress.NORMAL_ADDRESS
            isArchived `should be` false
        }
    }

    @Test
    fun `unarchive, clears archived`() {
        LegacyAddress().apply {
            archive()
            unarchive()
            isArchived `should be` false
        }
    }
}