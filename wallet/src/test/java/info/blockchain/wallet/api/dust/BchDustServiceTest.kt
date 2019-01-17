package info.blockchain.wallet.api.dust

import com.blockchain.koin.modules.walletApiServiceTestModule
import com.blockchain.koin.walletModule
import com.blockchain.network.initRule
import com.blockchain.network.modules.apiModule
import com.blockchain.testutils.rxInit
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ApiCode
import io.fabric8.mockwebserver.DefaultMockServer
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.standalone.StandAloneContext
import org.koin.standalone.inject
import org.koin.test.AutoCloseKoinTest

class BchDustServiceTest : AutoCloseKoinTest() {

    private val server = DefaultMockServer()

    @get:Rule
    val initMockServer = server.initRule()

    @get:Rule
    val initRx = rxInit {
        ioTrampoline()
    }

    private val subject: DustService by inject()
    private val apiCode: ApiCode by inject()

    @Before
    fun startKoin() {
        StandAloneContext.startKoin(
            listOf(
                apiModule,
                walletModule,
                walletApiServiceTestModule(server)
            )
        )
    }

    @Test
    fun `get dust returns input`() {
        server.expect().get().withPath("/bch/dust?api_code=${apiCode.apiCode}")
            .andReturn(
                200,
                """
{
  "tx_hash": "fd208b67abd52eb417cce9a1886f29342e3577a4d1f9c87fbb11ca21e6fc3a81",
  "tx_hash_big_endian": "813afce621ca11bb7fc8f9d1a477352e34296f88a1e9cc17b42ed5ab678b20fd",
  "tx_index": 0,
  "tx_output_n": 26,
  "script": "00",
  "value": 546,
  "value_hex": "00000222",
  "confirmations": 1,
  "output_script": "76a914757666a692b3676fef9df7d0f61d415012555f6288ac",
  "lock_secret": "b812995e2ca64c69bdd9187f2c26ab3b"
}
"""
            )
            .once()

        subject.getDust(CryptoCurrency.BCH)
            .test()
            .assertNoErrors()
            .values()
            .single()
            .apply {
                txHash `should equal` "fd208b67abd52eb417cce9a1886f29342e3577a4d1f9c87fbb11ca21e6fc3a81"
                txHashBigEndian `should equal` "813afce621ca11bb7fc8f9d1a477352e34296f88a1e9cc17b42ed5ab678b20fd"
                txIndex `should equal` 0L
                txOutputN `should equal` 26L
                script `should equal` "00"
                value `should equal` 546.toBigInteger()
                valueHex `should equal` "00000222"
                confirmations `should equal` 1
                outputScript `should equal` "76a914757666a692b3676fef9df7d0f61d415012555f6288ac"
                lockSecret `should equal` "b812995e2ca64c69bdd9187f2c26ab3b"
            }
    }
}