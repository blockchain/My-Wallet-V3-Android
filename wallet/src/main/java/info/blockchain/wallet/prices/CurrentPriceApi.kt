package info.blockchain.wallet.prices

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import java.math.BigDecimal

interface CurrentPriceApi {

    fun currentPrice(base: CryptoCurrency, quoteFiatCode: String): Single<BigDecimal>
}