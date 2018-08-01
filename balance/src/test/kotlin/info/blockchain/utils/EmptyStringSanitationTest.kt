package info.blockchain.utils

import org.amshove.kluent.`should equal`
import org.junit.Test

class EmptyStringSanitationTest {

    @Test
    fun `sanitise empty`() {
        "".sanitiseEmptyNumber() `should equal` "0"
    }

    @Test
    fun `sanitise non empty, number`() {
        "1.1".sanitiseEmptyNumber() `should equal` "1.1"
    }

    @Test
    fun `sanitise non empty, non-number`() {
        "x".sanitiseEmptyNumber() `should equal` "x"
    }
}
