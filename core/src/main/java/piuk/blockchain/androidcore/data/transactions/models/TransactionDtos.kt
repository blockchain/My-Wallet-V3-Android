package piuk.blockchain.androidcore.data.transactions.models

import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.androidcore.data.currency.CryptoCurrency
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import java.math.BigInteger

abstract class Displayable {

    abstract val cryptoCurrency: CryptoCurrency
    abstract val direction: TransactionSummary.Direction
    abstract val timeStamp: Long
    abstract val total: BigInteger
    abstract val fee: BigInteger
    abstract val hash: String
    abstract val inputsMap: HashMap<String, BigInteger>
    abstract val outputsMap: HashMap<String, BigInteger>
    open val confirmations = 0
    open val watchOnly: Boolean = false
    open val doubleSpend: Boolean = false
    open val isPending: Boolean = false
    open var totalDisplayableCrypto: String? = null
    open var totalDisplayableFiat: String? = null
    open var note: String? = null
}

data class EthDisplayable(
    private val combinedEthModel: CombinedEthModel,
    private val ethTransaction: EthTransaction,
    private val blockHeight: Long
) : Displayable() {

    override val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.ETHER
    override val direction: TransactionSummary.Direction
        get() = when {
            combinedEthModel.getAccounts()[0] == ethTransaction.to
                && combinedEthModel.getAccounts()[0] == ethTransaction.from -> TransactionSummary.Direction.TRANSFERRED
            combinedEthModel.getAccounts().contains(ethTransaction.from) -> TransactionSummary.Direction.SENT
            else -> TransactionSummary.Direction.RECEIVED
        }
    override val timeStamp: Long
        get() = ethTransaction.timeStamp
    override val total: BigInteger
        get() = when (direction) {
            TransactionSummary.Direction.RECEIVED -> ethTransaction.value
            else -> ethTransaction.value.plus(fee)
        }
    override val fee: BigInteger
        get() = ethTransaction.gasUsed.multiply(ethTransaction.gasPrice)
    override val hash: String
        get() = ethTransaction.hash
    override val inputsMap: HashMap<String, BigInteger>
        get() = HashMap<String, BigInteger>().apply {
            put(ethTransaction.from, ethTransaction.value)
        }
    override val outputsMap: HashMap<String, BigInteger>
        get() = HashMap<String, BigInteger>().apply {
            put(ethTransaction.to, ethTransaction.value)
        }
    override val confirmations: Int
        get() = (blockHeight - ethTransaction.blockNumber).toInt()
}

data class BtcDisplayable(
    private val transactionSummary: TransactionSummary
) : Displayable() {

    override val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.BTC
    override val direction: TransactionSummary.Direction
        get() = transactionSummary.direction
    override val timeStamp: Long
        get() = transactionSummary.time
    override val total: BigInteger
        get() = transactionSummary.total
    override val fee: BigInteger
        get() = transactionSummary.fee
    override val hash: String
        get() = transactionSummary.hash
    override val inputsMap: HashMap<String, BigInteger>
        get() = transactionSummary.inputsMap
    override val outputsMap: HashMap<String, BigInteger>
        get() = transactionSummary.outputsMap
    override val confirmations: Int
        get() = transactionSummary.confirmations
    override val watchOnly: Boolean
        get() = transactionSummary.isWatchOnly
    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend
    override val isPending: Boolean
        get() = transactionSummary.isPending
}

data class BchDisplayable(
    private val transactionSummary: TransactionSummary
) : Displayable() {

    override val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.BCH
    override val direction: TransactionSummary.Direction
        get() = transactionSummary.direction
    override val timeStamp: Long
        get() = transactionSummary.time
    override val total: BigInteger
        get() = transactionSummary.total
    override val fee: BigInteger
        get() = transactionSummary.fee
    override val hash: String
        get() = transactionSummary.hash
    override val inputsMap: HashMap<String, BigInteger>
        get() = transactionSummary.inputsMap
    override val outputsMap: HashMap<String, BigInteger>
        get() = transactionSummary.outputsMap
    override val confirmations: Int
        get() = transactionSummary.confirmations
    override val watchOnly: Boolean
        get() = transactionSummary.isWatchOnly
    override val doubleSpend: Boolean
        get() = transactionSummary.isDoubleSpend
    override val isPending: Boolean
        get() = transactionSummary.isPending
}