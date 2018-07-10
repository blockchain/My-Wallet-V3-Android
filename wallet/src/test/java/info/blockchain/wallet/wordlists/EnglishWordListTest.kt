package info.blockchain.wallet.wordlists

import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.security.MessageDigest

class EnglishWordListTest {

    @Test
    fun `sha-256 of bytes of wordlist file is as expected`() {
        readAllBytes("wordlist/en_US.txt").hashSha256() `should equal`
            "2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda"
    }

    private fun ByteArray.hashSha256() =
        Hex.toHexString(MessageDigest.getInstance("SHA-256").digest(this))

    private fun readAllBytes(resourceName: String): ByteArray {
        val resourceAsStream = javaClass.classLoader.getResourceAsStream(resourceName)
        resourceAsStream `should not be` null
        return resourceAsStream.readBytes()
    }
}