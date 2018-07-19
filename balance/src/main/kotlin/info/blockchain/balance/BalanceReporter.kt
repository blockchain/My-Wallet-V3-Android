package info.blockchain.balance

interface BalanceReporter {
    fun entireBalance(): CryptoValue
    fun watchOnlyBalance(): CryptoValue
    fun importedAddressBalance(): CryptoValue
    fun addressBalance(address: String): CryptoValue
}
