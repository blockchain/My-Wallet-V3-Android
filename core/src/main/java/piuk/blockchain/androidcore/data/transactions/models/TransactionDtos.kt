package piuk.blockchain.androidcore.data.transactions.models

import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.utils.annotations.Mockable
import java.math.BigInteger

abstract class Displayable {

    abstract val cryptoCurrency: CryptoCurrencies
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

    override fun toString(): String = "cryptoCurrency = $cryptoCurrency" +
        "direction  = $direction " +
        "timeStamp  = $timeStamp " +
        "total  = $total " +
        "fee  = $fee " +
        "hash  = $hash " +
        "inputsMap  = $inputsMap " +
        "outputsMap  = $outputsMap " +
        "confirmations  = $confirmations " +
        "watchOnly  = $watchOnly " +
        "doubleSpend  = $doubleSpend " +
        "isPending  = $isPending " +
        "totalDisplayableCrypto  = $totalDisplayableCrypto " +
        "totalDisplayableFiat  = $totalDisplayableFiat " +
        "note = $note"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Displayable?

        return this.cryptoCurrency == that?.cryptoCurrency &&
            this.direction == that.direction &&
            this.timeStamp == that.timeStamp &&
            this.total == that.total &&
            this.fee == that.fee &&
            this.hash == that.hash &&
            this.inputsMap == that.inputsMap &&
            this.outputsMap == that.outputsMap &&
            this.confirmations == that.confirmations &&
            this.watchOnly == that.watchOnly &&
            this.doubleSpend == that.doubleSpend &&
            this.isPending == that.isPending &&
            this.totalDisplayableCrypto == that.totalDisplayableCrypto &&
            this.totalDisplayableFiat == that.totalDisplayableFiat &&
            this.note == that.note
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + cryptoCurrency.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + total.hashCode()
        result = 31 * result + fee.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + inputsMap.hashCode()
        result = 31 * result + outputsMap.hashCode()
        result = 31 * result + confirmations.hashCode()
        result = 31 * result + watchOnly.hashCode()
        result = 31 * result + doubleSpend.hashCode()
        result = 31 * result + (totalDisplayableCrypto?.hashCode() ?: 0)
        result = 31 * result + (totalDisplayableFiat?.hashCode() ?: 0)
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }
}

@Mockable
class EthDisplayable(
    private val combinedEthModel: CombinedEthModel,
    private val ethTransaction: EthTransaction,
    private val blockHeight: Long
) : Displayable() {

    override val cryptoCurrency: CryptoCurrencies
        get() = CryptoCurrencies.ETHER
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

@Mockable
class BtcDisplayable(
    private val transactionSummary: TransactionSummary
) : Displayable() {

    override val cryptoCurrency: CryptoCurrencies
        get() = CryptoCurrencies.BTC
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

@Mockable
class BchDisplayable(
    private val transactionSummary: TransactionSummary
) : Displayable() {

    override val cryptoCurrency: CryptoCurrencies
        get() = CryptoCurrencies.BCH
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