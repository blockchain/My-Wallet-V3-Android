package piuk.blockchain.androidcore.data.settings

import org.amshove.kluent.`should equal to`
import org.junit.Test

class PhoneNumberTest {

    @Test
    fun `should return sanitized string`() {
        PhoneNumber("+44 (123) 456-789").sanitized `should equal to` "+44123456789"
    }

    @Test
    fun `should return raw string`() {
        PhoneNumber("+44 (123) 456-789").raw `should equal to` "+44 (123) 456-789"
    }

    @Test
    fun `short phone number should return invalid`() {
        PhoneNumber("+123456").isValid `should equal to` false
    }

    @Test
    fun `full phone number should return valid`() {
        PhoneNumber("+1234567890").isValid `should equal to` true
    }
}
