package piuk.blockchain.androidcore.data.payload

import info.blockchain.api.data.Balance
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.metadata.MetadataNodeFactory
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.spongycastle.crypto.InvalidCipherTextException
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.util.ArrayList
import java.util.LinkedHashMap
import javax.inject.Inject

@PresenterScope
class PayloadDataManager @Inject constructor(
    private val payloadService: PayloadService,
    private val privateKeyFactory: PrivateKeyFactory,
    private val payloadManager: PayloadManager,
    environmentConfig: EnvironmentConfig,
    rxBus: RxBus
) {

    private val rxPinning: RxPinning = RxPinning(rxBus)
    private val networkParameters = environmentConfig.bitcoinNetworkParameters

    // /////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS AND PROPERTIES
    // /////////////////////////////////////////////////////////////////////////

    val accounts: List<Account>
        get() = wallet?.hdWallets?.get(0)?.accounts ?: emptyList()

    var legacyAddresses: List<LegacyAddress>
        get() = wallet?.legacyAddressList ?: emptyList()
        set(addresses) {
            wallet!!.legacyAddressList = addresses
        }

    val legacyAddressStringList: List<String>
        get() = wallet?.legacyAddressStringList ?: emptyList()

    val watchOnlyAddressStringList: List<String>
        get() = wallet?.watchOnlyAddressStringList ?: emptyList()

    val wallet: Wallet?
        get() = payloadManager.payload

    val defaultAccountIndex: Int
        get() = wallet?.hdWallets?.get(0)?.defaultAccountIdx ?: 0

    val defaultAccount: Account
        get() = wallet!!.hdWallets[0].getAccount(defaultAccountIndex)

    val payloadChecksum: String?
        get() = payloadManager.payloadChecksum

    var tempPassword: String?
        get() = payloadManager.tempPassword
        set(password) {
            payloadManager.tempPassword = password
        }

    val walletBalance: BigInteger
        get() = payloadManager.walletBalance

    val importedAddressesBalance: BigInteger
        get() = payloadManager.importedAddressesBalance

    val isDoubleEncrypted: Boolean
        get() = wallet!!.isDoubleEncryption

    val isBackedUp: Boolean
        get() = (
            payloadManager.payload != null &&
                payloadManager.payload!!.hdWallets != null &&
                payloadManager.payload!!.hdWallets[0].isMnemonicVerified
            )

    val mnemonic: List<String>
        get() = payloadManager.payload!!.hdWallets[0].mnemonic

    val guid: String
        get() = wallet!!.guid

    val sharedKey: String
        get() = wallet!!.sharedKey

    // /////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a [DecryptionException] if the password is incorrect, otherwise can return a
     * [HDWalletException] which should be regarded as fatal.
     *
     * @param payload The payload String to be decrypted
     * @param password The user's password
     * @return A [Completable] object
     */
    fun initializeFromPayload(payload: String, password: String): Completable =
        rxPinning.call {
            payloadService.initializeFromPayload(networkParameters, payload, password)
        }.applySchedulers()

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the [PayloadDataManager].
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param password The user's choice of password
     * @return An [Observable] wrapping a [Wallet] object
     */
    fun restoreHdWallet(
        mnemonic: String,
        walletName: String,
        email: String,
        password: String
    ): Observable<Wallet> = rxPinning.call<Wallet> {
        payloadService.restoreHdWallet(
            mnemonic,
            walletName,
            email,
            password
        )
    }.applySchedulers()

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @return An [Observable] wrapping a [Wallet] object
     */
    fun createHdWallet(password: String, walletName: String, email: String): Observable<Wallet> =
        rxPinning.call<Wallet> { payloadService.createHdWallet(password, walletName, email) }
            .applySchedulers()

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's  password.
     *
     * @param sharedKey The shared key as a String
     * @param guid The user's GUID
     * @param password The user's password
     * @return A [Completable] object
     */
    fun initializeAndDecrypt(sharedKey: String, guid: String, password: String): Completable =
        rxPinning.call {
            payloadService.initializeAndDecrypt(networkParameters, sharedKey, guid, password)
        }.applySchedulers()

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A [Completable] object
     */
    fun handleQrCode(data: String): Completable =
        rxPinning.call { payloadService.handleQrCode(networkParameters, data) }
            .applySchedulers()

    /**
     * Upgrades a Wallet from V2 to V3 and saves it with the server. If saving is unsuccessful or
     * some other part fails, this will propagate an Exception.
     *
     * @param secondPassword An optional second password if the user has one
     * @param defaultAccountName A required name for the default account
     * @return A [Completable] object
     */
    fun upgradeV2toV3(secondPassword: String?, defaultAccountName: String): Completable =
        rxPinning.call { payloadService.upgradeV2toV3(secondPassword, defaultAccountName) }
            .applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // SYNC METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [Completable] which saves the current payload to the server.
     *
     * @return A [Completable] object
     */
    fun syncPayloadWithServer(): Completable =
        rxPinning.call { payloadService.syncPayloadWithServer() }
            .applySchedulers()

    /**
     * Returns a [Completable] which saves the current payload to the server whilst also
     * forcing the sync of the user's public keys. This method generates 20 addresses per Account,
     * so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A [Completable] object
     */
    fun syncPayloadAndPublicKeys(): Completable =
        rxPinning.call { payloadService.syncPayloadAndPublicKeys() }
            .applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // TRANSACTION METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns [Completable] which updates transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A [Completable] object
     * @see IgnorableDefaultObserver
     */
    fun updateAllTransactions(): Completable =
        rxPinning.call { payloadService.updateAllTransactions() }
            .applySchedulers()

    /**
     * Returns a [Completable] which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A [Completable] object
     * @see IgnorableDefaultObserver
     */
    fun updateAllBalances(): Completable = rxPinning.call { payloadService.updateAllBalances() }
        .applySchedulers()

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes Transaction notes
     * @return A [Completable] object
     */
    fun updateTransactionNotes(transactionHash: String, notes: String): Completable =
        rxPinning.call { payloadService.updateTransactionNotes(transactionHash, notes) }
            .applySchedulers()

    // /////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their addresses.
     *
     * @param addresses A List of addresses as Strings
     * @return A [LinkedHashMap]
     */
    fun getBalanceOfAddresses(addresses: List<String>): Observable<LinkedHashMap<String, Balance>> =
        rxPinning.call<LinkedHashMap<String, Balance>> {
            payloadService.getBalanceOfAddresses(addresses)
        }.applySchedulers()

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their Bitcoin Cash
     * addresses.
     *
     * @param addresses A List of Bitcoin cash addresses as Strings
     * @return A [LinkedHashMap]
     */
    fun getBalanceOfBchAddresses(addresses: List<String>): Observable<LinkedHashMap<String, Balance>> =
        rxPinning.call<LinkedHashMap<String, Balance>> {
            payloadService.getBalanceOfBchAddresses(addresses)
        }.applySchedulers()

    /**
     * Converts any address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as legacy address.
     * @return Either the label associated with the address, or the original address
     */
    fun addressToLabel(address: String): String = payloadManager.getLabelFromAddress(address)

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddress(accountIndex: Int): Observable<String> {
        val account = accounts[accountIndex]
        return getNextReceiveAddress(account)
    }

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param account The [Account] for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddress(account: Account): Observable<String> =
        Observable.fromCallable { payloadManager.getNextReceiveAddress(account) }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())

    /**
     * Returns the position of the next Receive address for a given [Account]
     *
     * @param account The [Account] for which you want an address to be found
     * @return The position of the next receive address
     */
    fun getNextReceiveAddressPosition(account: Account): Int =
        payloadManager.getPositionOfNextReceiveAddress(account)

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @param label Label used to reserve address
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddressAndReserve(accountIndex: Int, label: String): Observable<String> {
        val account = accounts[accountIndex]
        return Observable.fromCallable {
            payloadManager.getNextReceiveAddressAndReserve(account, label)
        }.subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Returns the next Receive address for a given [Account]
     *
     * @param account The account for which you want an address to be generated
     * @param label Label used to reserve address
     * @return An [Observable] wrapping the receive address
     */
    fun getNextReceiveAddressAndReserve(account: Account, label: String): Observable<String> =
        Observable.fromCallable {
            payloadManager.getNextReceiveAddressAndReserve(account, label)
        }.subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())

    /**
     * Returns the next Change address for a given account index.
     *
     * @param accountIndex The index of the account for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextChangeAddress(accountIndex: Int): Observable<String> {
        val account = accounts[accountIndex]
        return getNextChangeAddress(account)
    }

    /**
     * Returns the next Change address for a given [Account].
     *
     * @param account The [Account] for which you want an address to be generated
     * @return An [Observable] wrapping the receive address
     */
    fun getNextChangeAddress(account: Account): Observable<String> =
        Observable.fromCallable { payloadManager.getNextChangeAddress(account) }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())

    /**
     * Returns an [ECKey] for a given [LegacyAddress], optionally with a second password
     * should the private key be encrypted.
     *
     * @param legacyAddress The [LegacyAddress] to generate an Elliptic Curve Key for
     * @param secondPassword An optional second password, necessary if the private key is ebcrypted
     * @return An Elliptic Curve Key object [ECKey]
     * @throws UnsupportedEncodingException Thrown if the private key is formatted incorrectly
     * @throws DecryptionException Thrown if the supplied password is wrong
     * @throws InvalidCipherTextException Thrown if there's an issue decrypting the private key
     * @see LegacyAddress.isPrivateKeyEncrypted
     */
    @Throws(
        UnsupportedEncodingException::class,
        DecryptionException::class,
        InvalidCipherTextException::class
    )
    fun getAddressECKey(legacyAddress: LegacyAddress, secondPassword: String?): ECKey? =
        payloadManager.getAddressECKey(legacyAddress, secondPassword)

    /**
     * Derives new [Account] from the master seed
     *
     * @param accountLabel A label for the account
     * @param secondPassword An optional double encryption password
     * @return An [Observable] wrapping the newly created Account
     */
    fun createNewAccount(accountLabel: String, secondPassword: String?): Observable<Account> =
        rxPinning.call<Account> {
            payloadService.createNewAccount(networkParameters, accountLabel, secondPassword)
        }.applySchedulers()

    /**
     * Sets a private key for an associated [LegacyAddress] which is already in the [ ] as a watch only address
     *
     * @param key An [ECKey]
     * @param secondPassword An optional double encryption password
     * @return An [Observable] representing a successful save
     */
    fun setKeyForLegacyAddress(key: ECKey, secondPassword: String?): Observable<LegacyAddress> =
        rxPinning.call<LegacyAddress> {
            payloadService.setKeyForLegacyAddress(key, secondPassword)
        }.applySchedulers()

    /**
     * Allows you to add a [LegacyAddress] to the [Wallet]
     *
     * @param legacyAddress The new address
     * @return A [Completable] object representing a successful save
     */
    fun addLegacyAddress(legacyAddress: LegacyAddress): Completable =
        rxPinning.call { payloadService.addLegacyAddress(legacyAddress) }
            .applySchedulers()

    /**
     * Allows you to propagate changes to a [LegacyAddress] through the [Wallet]
     *
     * @param legacyAddress The updated address
     * @return A [Completable] object representing a successful save
     */
    fun updateLegacyAddress(legacyAddress: LegacyAddress): Completable =
        rxPinning.call { payloadService.updateLegacyAddress(legacyAddress) }
            .applySchedulers()

    /**
     * Returns an Elliptic Curve key for a given private key
     *
     * @param format The format of the private key
     * @param data The private key from which to derive the ECKey
     * @return An [ECKey]
     * @see PrivateKeyFactory
     */
    fun getKeyFromImportedData(format: String, data: String): Observable<ECKey> =
        Observable.fromCallable { privateKeyFactory.getKey(format, data) }
            .applySchedulers()

    /**
     * Returns the balance of an address. If the address isn't found in the address map object, the
     * method will return [BigInteger.ZERO] instead of a null object.
     *
     * @param address The address whose balance you wish to query
     * @return A [BigInteger] representing the total funds in the address
     */
    fun getAddressBalance(address: String): BigInteger = payloadManager.getAddressBalance(address)

    /**
     * Allows you to generate a receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param account The [Account] you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     * you wish to generate
     * @return A bitcoin address
     */
    fun getReceiveAddressAtPosition(account: Account, position: Int): String? =
        payloadManager.getReceiveAddressAtPosition(account, position)

    /**
     * Allows you to get an address from any given point on the receive chain.
     *
     * @param account The [Account] you wish to generate an address from
     * @param position What position on the chain the address you wish to create is
     * @return A bitcoin address
     */
    fun getReceiveAddressAtArbitraryPosition(account: Account, position: Int): String? =
        payloadManager.getReceiveAddressAtArbitraryPosition(account, position)

    /**
     * Updates the balance of the address as well as that of the entire wallet. To be called after a
     * successful sweep to ensure that balances are displayed correctly before syncing the wallet.
     *
     * @param address An address from which you've just spent funds
     * @param spentAmount The spent amount as a long
     * @throws Exception Thrown if the address isn't found
     */
    @Throws(Exception::class)
    fun subtractAmountFromAddressBalance(address: String, spentAmount: Long) {
        payloadManager.subtractAmountFromAddressBalance(address, BigInteger.valueOf(spentAmount))
    }

    /**
     * Increments the index on the receive chain for an [Account] object.
     *
     * @param account The [Account] you wish to increment
     */
    fun incrementReceiveAddress(account: Account) {
        payloadManager.incrementNextReceiveAddress(account)
    }

    /**
     * Increments the index on the change chain for an [Account] object.
     *
     * @param account The [Account] you wish to increment
     */
    fun incrementChangeAddress(account: Account) {
        payloadManager.incrementNextChangeAddress(account)
    }

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     *
     * @param address The address you want to query as a String
     * @return An xPub as a String
     */
    fun getXpubFromAddress(address: String): String? = payloadManager.getXpubFromAddress(address)

    /**
     * Returns an xPub from a given [Account] index. This call is not index-safe, ie will
     * throw an [IndexOutOfBoundsException] if you choose an index which is greater than the
     * size of the Accounts list.
     *
     * @param index The index of the Account
     * @return An xPub as a String
     */
    fun getXpubFromIndex(index: Int): String = payloadManager.getXpubFromAccountIndex(index)

    /**
     * Returns true if the supplied address belongs to the user's wallet.
     *
     * @param address The address you want to query as a String
     * @return true if the address belongs to the user
     */
    fun isOwnHDAddress(address: String): Boolean = payloadManager.isOwnHDAddress(address)

    // /////////////////////////////////////////////////////////////////////////
    // CONTACTS/METADATA/IWCS/CRYPTO-MATRIX METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [MetadataNodeFactory] object which allows you to access the [DeterministicKey]
     * objects needed to initialise the Contacts service.
     *
     * @return An [Observable] wrapping a [MetadataNodeFactory]
     */
    fun getMetadataNodeFactory(): Observable<MetadataNodeFactory> =
        Observable.just(payloadManager.metadataNodeFactory)

    /**
     * Loads previously saved nodes from the Metadata service. If none are found, the [Observable] returns false.
     *
     * @return An [Observable] object wrapping a boolean value, representing successfully
     * loaded nodes
     */
    fun loadNodes(): Observable<Boolean> = rxPinning.call<Boolean> { payloadService.loadNodes() }
        .applySchedulers()

    /**
     * Generates the metadata and shared metadata nodes if necessary.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun generateNodes(): Completable = rxPinning.call { payloadService.generateNodes() }
        .applySchedulers()

    fun generateAndReturnNodes(): Observable<MetadataNodeFactory> =
        rxPinning.call { payloadService.generateNodes() }
            .andThen(getMetadataNodeFactory())
            .applySchedulers()

    /**
     * Registers the user's MDID with the metadata service.
     *
     * @return An [Observable] wrapping a [ResponseBody]
     */
    fun registerMdid(): Observable<ResponseBody> =
        rxPinning.call<ResponseBody> { payloadService.registerMdid() }
            .applySchedulers()

    /**
     * Unregisters the user's MDID from the metadata service.
     *
     * @return An [Observable] wrapping a [ResponseBody]
     */
    fun unregisterMdid(): Observable<ResponseBody> =
        rxPinning.call<ResponseBody> { payloadService.unregisterMdid() }
            .applySchedulers()

    fun getAccount(accountPosition: Int): Account =
        wallet!!.hdWallets[0].getAccount(accountPosition)

    fun getAccountForXPub(xPub: String): Account {
        return accounts.firstOrNull { it.xpub == xPub }
            ?: throw NullPointerException("Account not found for XPub")
    }

    /**
     * Returns the transaction notes for a given transaction hash. May return null if not found.
     *
     * @param txHash The Tx hash
     * @return A string representing the Tx note, which can be null
     */
    fun getTransactionNotes(txHash: String): String? = payloadManager.payload!!.txNotes[txHash]

    /**
     * Returns a list of [ECKey] objects for signing transactions.
     *
     * @param account The [Account] that you wish to send funds from
     * @param unspentOutputBundle A [SpendableUnspentOutputs] bundle for a given Account
     * @return A list of [ECKey] objects
     * @throws Exception Will be thrown if there are issues with the private keys
     */
    @Throws(Exception::class)
    fun getHDKeysForSigning(
        account: Account,
        unspentOutputBundle: SpendableUnspentOutputs
    ): List<ECKey> = wallet!!
        .hdWallets[0]
        .getHDKeysForSigning(account, unspentOutputBundle)

    // /////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns the index for an [Account], assuming that the supplied position was gotten from
     * a list of only those Accounts which are active.
     *
     * @param position The position of the [Account] that you want to select from a list of
     * active Accounts
     * @return The position of the [Account] within the full list of Accounts
     */
    fun getPositionOfAccountFromActiveList(position: Int): Int {
        val accounts = accounts
        var adjustedPosition = 0
        for (i in accounts.indices) {
            val account = accounts[i]
            if (!account.isArchived) {
                if (position == adjustedPosition) {
                    return i
                }
                adjustedPosition++
            }
        }

        return 0
    }

    /**
     * Returns the index for an [Account] in a list of active-only Accounts, where the
     * supplied `accountIndex` is the position of the Account in the full list of both active
     * and archived Accounts.
     *
     * @param accountIndex The position of an [Account] in the full list of Accounts
     * @return The Account's position within a list of active-only Accounts. Will be -1 if you
     * attempt to find the position of an archived Account
     */
    fun getPositionOfAccountInActiveList(accountIndex: Int): Int {
        // Filter accounts by active
        val activeAccounts = ArrayList<Account>()
        val accounts = accounts
        for (i in accounts.indices) {
            val account = accounts[i]
            if (!account.isArchived) {
                activeAccounts.add(account)
            }
        }

        // Find corrected position
        return activeAccounts.indexOf(accounts[accountIndex])
    }

    fun validateSecondPassword(secondPassword: String?): Boolean =
        payloadManager.validateSecondPassword(secondPassword)

    @Throws(Exception::class)
    fun decryptHDWallet(networkParameters: NetworkParameters, secondPassword: String?) {
        payloadManager.payload!!.decryptHDWallet(networkParameters, 0, secondPassword)
    }
}
