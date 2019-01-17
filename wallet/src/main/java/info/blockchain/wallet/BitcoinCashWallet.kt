package info.blockchain.wallet

import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.api.data.UnspentOutput
import info.blockchain.wallet.crypto.DeterministicAccount
import info.blockchain.wallet.crypto.DeterministicWallet
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.multiaddress.MultiAddressFactoryBch
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.BalanceManagerBch
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashTestNet3Params
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.ArrayList

@Suppress("unused")
open class BitcoinCashWallet : DeterministicWallet {

    private lateinit var balanceManager: BalanceManagerBch
    private lateinit var multiAddressFactory: MultiAddressFactoryBch

    private constructor(
        blockExplorer: BlockExplorer,
        params: NetworkParameters,
        coinPath: String,
        passphrase: String
    ) : super(params, coinPath, MNEMONIC_LENGTH, passphrase) {
        setupApi(blockExplorer)
    }

    private constructor(
        blockExplorer: BlockExplorer,
        params: NetworkParameters,
        coinPath: String,
        entropyHex: String,
        passphrase: String
    ) : super(params, coinPath, entropyHex, passphrase) {
        setupApi(blockExplorer)
    }

    private constructor(
        blockExplorer: BlockExplorer,
        params: NetworkParameters,
        coinPath: String,
        mnemonic: List<String>,
        passphrase: String
    ) : super(params, coinPath, mnemonic, passphrase) {
        setupApi(blockExplorer)
    }

    private constructor(blockExplorer: BlockExplorer, params: NetworkParameters) : super(params) {
        setupApi(blockExplorer)
    }

    private fun setupApi(blockExplorer: BlockExplorer) {
        this.balanceManager = BalanceManagerBch(blockExplorer)
        this.multiAddressFactory = MultiAddressFactoryBch(blockExplorer)
    }

    /**
     * Updates the state of the [BalanceManagerBch], which ingests the balances for each address or
     * xPub.
     *
     * @param legacyAddressList A list of [LegacyAddress] addresses
     * @param allAccountsAndAddresses A list of both xPubs from HD accounts and [LegacyAddress]
     * addresses
     */
    fun updateAllBalances(
        allAccountsAndAddresses: Set<String>,
        legacyAddressList: Set<String>
    ): Completable =
        if (isTestnet()) {
            // TODO(bch testnet explorer coming soon)
            Completable.complete()
        } else {
            Completable.fromCallable {
                balanceManager.updateAllBalances(allAccountsAndAddresses, legacyAddressList, emptySet())
            }
        }

    fun getAddressBalance(address: String): BigInteger =
        balanceManager.getAddressBalance(address)

    fun getWalletBalance(): BigInteger = balanceManager.walletBalance

    /**
     * Returns the balance of all imported addresses, excluding those belonging to
     * archived addresses.
     */
    fun getImportedAddressBalance(): BigInteger =
        balanceManager.importedAddressesBalance

    /**
     *
     * @param legacyAddressList A list of all xpubs and legacy addresses
     * @param watchOnly A list of watch-only legacy addresses
     * @param activeXpubs A list of active xPubs addresses.
     * @param context Xpub or legacy address. Used to fetch transaction only relating to this address.
     * @param limit Maximum amount of transactions fetched
     * @param offset Page offset
     * @return All wallet transactions, all legacy transactions, or transaction relating to a single context/address
     */
    fun getTransactions(
        legacyAddressList: List<String>?,
        watchOnly: List<String>,
        activeXpubs: List<String>,
        context: String?,
        limit: Int,
        offset: Int
    ): MutableList<TransactionSummary> =

        if (isTestnet()) {
            // TODO(bch testnet explorer coming soon)
            mutableListOf()
        } else {
            multiAddressFactory.getAccountTransactions(
                activeXpubs,
                watchOnly,
                legacyAddressList,
                context,
                limit,
                offset,
                BCH_FORK_HEIGHT
            )
        }

    /**
     * Generates a Base58 Bitcoin Cash receive address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash receive address in Base58 format
     */
    fun getNextReceiveAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactory.getNextReceiveAddressIndex(xpub, listOf())
        return getReceiveBase58AddressAt(accountIndex, addressIndex)
    }

    /**
     * Generates a bech32 Bitcoin Cash receive address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash receive address in bech32 format
     */
    fun getNextReceiveCashAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactory.getNextReceiveAddressIndex(xpub, listOf())
        return getReceiveCashAddressAt(accountIndex, addressIndex)
    }

    /**
     * Generates a Base58 Bitcoin Cash change address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash change address in Base58 format
     */
    fun getNextChangeAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactory.getNextChangeAddressIndex(xpub)
        return getChangeBase58AddressAt(accountIndex, addressIndex)
    }

    /**
     * Generates a bech32 Bitcoin Cash change address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash change address in bech32 format
     */
    fun getNextChangeCashAddress(accountIndex: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactory.getNextChangeAddressIndex(xpub)
        return getChangeCashAddressAt(accountIndex, addressIndex)
    }

    /**
     * Allows you to generate a receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     *                 you wish to generate
     * @return A Bitcoin Cash receive address in Base58
     */
    fun getReceiveAddressAtPosition(accountIndex: Int, position: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactory.getNextReceiveAddressIndex(xpub, listOf())
        return getReceiveBase58AddressAt(accountIndex, addressIndex + position)
    }

    /**
     * Allows you to generate a change address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     *                 you wish to generate
     * @return A Bitcoin Cash change address in Base58
     */
    fun getChangeAddressAtPosition(accountIndex: Int, position: Int): String {
        val xpub = getAccountPubB58(accountIndex)
        val addressIndex = multiAddressFactory.getNextChangeAddressIndex(xpub)
        return getChangeBase58AddressAt(accountIndex, addressIndex + position)
    }

    /**
     * Allows you to generate a receive address from any given point on the receive chain.
     *
     * @param accountIndex The index of the account you wish to generate an address from
     * @param addressIndex What position on the chain the address you wish to create is
     * @return A Bitcoin Cash receive address in Base58 format
     */
    fun getReceiveAddressAtArbitraryPosition(accountIndex: Int, addressIndex: Int): String {
        return getReceiveBase58AddressAt(accountIndex, addressIndex)
    }

    /**
     * Allows you to generate a change address from any given point on the change chain.
     *
     * @param accountIndex The index of the account you wish to generate an address from
     * @param addressIndex What position on the chain the address you wish to create is
     * @return A Bitcoin Cash change address in Base58 format
     */
    fun getChangeAddressAtArbitraryPosition(accountIndex: Int, addressIndex: Int): String {
        return getChangeBase58AddressAt(accountIndex, addressIndex)
    }

    fun incrementNextReceiveAddress(xpub: String) {
        multiAddressFactory.incrementNextReceiveAddress(xpub, listOf())
    }

    fun incrementNextChangeAddress(xpub: String) {
        multiAddressFactory.incrementNextChangeAddress(xpub)
    }

    /**
     * Returns whether or not an address belongs to this wallet.
     * @param address The base58 address you want to query
     * @return
     */
    fun isOwnAddress(address: String) =
        multiAddressFactory.isOwnHDAddress(address)

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     * @param address The Bitcoin Cash base58 address you want to query
     * @return An xPub as a String
     */
    fun getXpubFromAddress(address: String): String? {
        return multiAddressFactory.getXpubFromAddress(address)
    }

    /**
     * Updates address balance as well as wallet balance in [BalanceManagerBch]. This is used to immediately update
     * balances after a successful transaction which speeds up the balance the UI reflects without
     * the need to wait for incoming websocket notification.
     *
     * @param amount The amount to be subtracted from the address's balance
     * @param address A valid Bitcoin cash address in base58 format
     */
    @Throws(Exception::class)
    fun subtractAmountFromAddressBalance(address: String, amount: BigInteger) {
        balanceManager.subtractAmountFromAddressBalance(address, amount)
    }

    @Throws(HDWalletException::class)
    fun getHDKeysForSigning(
        account: DeterministicAccount,
        unspentOutputs: List<UnspentOutput>
    ): List<ECKey> {

        if (!account.node.hasPrivKey())
            throw HDWalletException("Wallet private key unavailable. First decrypt with second password.")
        else {
            val keys = ArrayList<ECKey>()

            for (unspentOutput in unspentOutputs) {
                if (unspentOutput.xpub != null) {
                    val split =
                        unspentOutput.xpub.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    val chain = Integer.parseInt(split[1])
                    val addressIndex = Integer.parseInt(split[2])
                    val address = account.chains[chain]!!.getAddressAt(addressIndex)
                    keys.add(address.ecKey)
                }
            }

            return keys
        }
    }

    private fun isTestnet() = params == BitcoinCashTestNet3Params.get()

    companion object {

        /**
         * Coin parameters
         */
        private val log = LoggerFactory.getLogger(BitcoinCashWallet::class.java)
        const val BITCOIN_COIN_PATH = "M/44H/0H"
        const val BITCOINCASH_COIN_PATH = "M/44H/145H"
        const val MNEMONIC_LENGTH = 12
        const val BCH_FORK_HEIGHT = 478558

        /**
         * Coin metadata store
         */
        const val METADATA_TYPE_EXTERNAL = 7

        @Synchronized
        fun create(
            blockExplorer: BlockExplorer,
            params: NetworkParameters,
            coinPath: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(blockExplorer, params, coinPath, "")
        }

        @Synchronized
        fun create(
            blockExplorer: BlockExplorer,
            params: NetworkParameters,
            coinPath: String,
            passphrase: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(blockExplorer, params, coinPath, passphrase)
        }

        @Synchronized
        fun restore(
            blockExplorer: BlockExplorer,
            params: NetworkParameters,
            coinPath: String,
            entropyHex: String,
            passphrase: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(blockExplorer, params, coinPath, entropyHex, passphrase)
        }

        @Synchronized
        fun restore(
            blockExplorer: BlockExplorer,
            params: NetworkParameters,
            coinPath: String,
            mnemonic: List<String>,
            passphrase: String
        ): BitcoinCashWallet {
            return BitcoinCashWallet(blockExplorer, params, coinPath, mnemonic, passphrase)
        }

        @Synchronized
        fun createWatchOnly(
            blockExplorer: BlockExplorer,
            params: NetworkParameters
        ): BitcoinCashWallet {
            return BitcoinCashWallet(blockExplorer, params)
        }
    }
}
