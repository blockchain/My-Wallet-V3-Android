package com.blockchain.datamanagers.fees

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import java.math.BigInteger

fun FeeDataManager.getFeeOptions(cryptoCurrency: CryptoCurrency): Single<out NetworkFees> =
    when (cryptoCurrency) {
        CryptoCurrency.BTC -> btcFeeOptions.map {
            BitcoinLikeFees(
                it.regularFee,
                it.priorityFee
            )
        }
        CryptoCurrency.BCH -> bchFeeOptions.map {
            BitcoinLikeFees(
                it.regularFee,
                it.priorityFee
            )
        }
        CryptoCurrency.ETHER -> ethFeeOptions.map {
            EthereumFees(
                it.regularFee,
                it.gasLimit
            )
        }
        else -> throw NotImplementedError("Cryptocurrency not yet supported")
    }.singleOrError()

sealed class NetworkFees

data class BitcoinLikeFees(
    private val regularFeePerByte: Long,
    private val priorityFeePerByte: Long
) : NetworkFees() {

    val regularFeePerKb: BigInteger = (regularFeePerByte * 1000).toBigInteger()

    val priorityFeePerKb: BigInteger = (priorityFeePerByte * 1000).toBigInteger()
}

data class EthereumFees(
    private val gasPriceGwei: Long,
    private val gasLimitGwei: Long
) : NetworkFees() {

    val absoluteFee: CryptoValue =
        CryptoValue.etherFromWei((gasPriceGwei * gasLimitGwei).gweiToWei())

    val gasPriceWei: BigInteger = gasPriceGwei.gweiToWei()

    val gasLimitWei: BigInteger = gasLimitGwei.gweiToWei()
}

fun Long.gweiToWei(): BigInteger =
    Convert.toWei(this.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()

sealed class FeeType {
    object Regular : FeeType()
    object Priority : FeeType()
}