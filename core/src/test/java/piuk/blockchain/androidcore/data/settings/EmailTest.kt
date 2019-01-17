package piuk.blockchain.androidcore.data.settings

import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Test

class EmailTest {

    @Test
    fun `assert equals`() {
        Email("abc@def.com", verified = false) `should equal` Email("abc@def.com", verified = false)
    }

    @Test
    fun `assert not equals by verified`() {
        Email("abc@def.com", verified = false) `should not equal` Email("abc@def.com", verified = true)
    }

    @Test
    fun `assert not equals by address`() {
        Email("abc@def.com", verified = false) `should not equal` Email("def@abc.com", verified = false)
    }
}
