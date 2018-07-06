package piuk.blockchain.androidcore.data.payload

import info.blockchain.api.data.Balance
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.exceptions.Exceptions
import okhttp3.ResponseBody
import org.bitcoinj.core.ECKey
import piuk.blockchain.androidcore.utils.annotations.Mockable
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import piuk.blockchain.androidcore.utils.rxjava.IgnorableDefaultObserver
import java.util.LinkedHashMap
import javax.inject.Inject

@Mockable
class PayloadService @Inject constructor(private val payloadManager: PayloadManager) {

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
    @WebRequest
    internal fun initializeFromPayload(payload: String, password: String): Completable =
        Completable.fromCallable {
            payloadManager.initializeAndDecryptFromPayload(payload, password)
        }

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the [PayloadDataManager].
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param password The user's choice of password
     * @return An [Observable] wrapping the [Wallet] object
     */
    @WebRequest
    internal fun restoreHdWallet(
        mnemonic: String,
        walletName: String,
        email: String,
        password: String
    ): Observable<Wallet> = Observable.fromCallable {
        payloadManager.recoverFromMnemonic(
            mnemonic,
            walletName,
            email,
            password
        )
    }

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @return An [Observable] wrapping the [Wallet] object
     */
    @WebRequest
    internal fun createHdWallet(
        password: String,
        walletName: String,
        email: String
    ): Observable<Wallet> =
        Observable.fromCallable { payloadManager.create(walletName, email, password) }

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's password.
     *
     * @param sharedKey The shared key as a String
     * @param guid The user's GUID
     * @param password The user's password
     * @return A [Completable] object
     */
    @WebRequest
    internal fun initializeAndDecrypt(
        sharedKey: String,
        guid: String,
        password: String
    ): Completable = Completable.fromCallable {
        payloadManager.initializeAndDecrypt(sharedKey, guid, password)
    }

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A [Completable] object
     */
    @WebRequest
    internal fun handleQrCode(data: String): Completable = Completable.fromCallable {
        payloadManager.initializeAndDecryptFromQR(data)
    }

    /**
     * Upgrades a Wallet from V2 to V3 and saves it with the server. If saving is unsuccessful or
     * some other part fails, this will propagate an Exception.
     *
     * @param secondPassword An optional second password if the user has one
     * @param defaultAccountName A required name for the default account
     * @return A [Completable] object
     */
    @WebRequest
    internal fun upgradeV2toV3(secondPassword: String?, defaultAccountName: String): Completable =
        Completable.fromCallable {
            if (!payloadManager.upgradeV2PayloadToV3(secondPassword, defaultAccountName)) {
                throw Exceptions.propagate(Throwable("Upgrade wallet failed"))
            }
        }

    // /////////////////////////////////////////////////////////////////////////
    // SYNC METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [Completable] which saves the current payload to the server.
     *
     * @return A [Completable] object
     */
    @WebRequest
    internal fun syncPayloadWithServer(): Completable = Completable.fromCallable {
        if (!payloadManager.save()) throw ApiException("Sync failed")
    }

    /**
     * Returns a [Completable] which saves the current payload to the server whilst also
     * forcing the sync of the user's keys. This method generates 20 addresses per [Account],
     * so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A [Completable] object
     */
    @WebRequest
    internal fun syncPayloadAndPublicKeys(): Completable = Completable.fromCallable {
        if (!payloadManager.saveAndSyncPubKeys()) throw ApiException("Sync failed")
    }

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
    @WebRequest
    internal fun updateAllTransactions(): Completable = Completable.fromCallable {
        payloadManager.getAllTransactions(50, 0)
    }

    /**
     * Returns a [Completable] which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A [Completable] object
     * @see IgnorableDefaultObserver
     */
    @WebRequest
    internal fun updateAllBalances(): Completable = Completable.fromCallable {
        payloadManager.updateAllBalances()
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes Transaction notes
     * @return A [Completable] object
     */
    @WebRequest
    internal fun updateTransactionNotes(transactionHash: String, notes: String): Completable {
        payloadManager.payload!!.txNotes[transactionHash] = notes
        return syncPayloadWithServer()
    }

    // /////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their addresses.
     *
     * @param addresses A List of addresses as Strings
     * @return A [LinkedHashMap]
     */
    @WebRequest
    internal fun getBalanceOfAddresses(addresses: List<String>): Observable<LinkedHashMap<String, Balance>> =
        Observable.fromCallable { payloadManager.getBalanceOfAddresses(addresses) }

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their Bitcoin cash
     * addresses.
     *
     * @param addresses A List of Bitcoin Cash addresses as Strings
     * @return A [LinkedHashMap]
     */
    @WebRequest
    internal fun getBalanceOfBchAddresses(addresses: List<String>): Observable<LinkedHashMap<String, Balance>> =
        Observable.fromCallable { payloadManager.getBalanceOfBchAddresses(addresses) }

    /**
     * Derives new [Account] from the master seed
     *
     * @param accountLabel A label for the account
     * @param secondPassword An optional double encryption password
     * @return An [Observable] wrapping the newly created Account
     */
    @WebRequest
    internal fun createNewAccount(
        accountLabel: String,
        secondPassword: String?
    ): Observable<Account> =
        Observable.fromCallable { payloadManager.addAccount(accountLabel, secondPassword) }

    /**
     * Sets a private key for an associated [LegacyAddress] which is already in the [Wallet] as a
     * watch only address
     *
     * @param key An [ECKey]
     * @param secondPassword An optional double encryption password
     * @return An [Observable] representing a successful save
     */
    @WebRequest
    internal fun setKeyForLegacyAddress(
        key: ECKey,
        secondPassword: String?
    ): Observable<LegacyAddress> = Observable.fromCallable {
        payloadManager.setKeyForLegacyAddress(key, secondPassword)
    }

    /**
     * Allows you to add a [LegacyAddress] to the [Wallet]
     *
     * @param legacyAddress The new address
     * @return A [Completable] object representing a successful save
     */
    @WebRequest
    internal fun addLegacyAddress(legacyAddress: LegacyAddress): Completable =
        Completable.fromCallable {
            payloadManager.addLegacyAddress(legacyAddress)
        }

    /**
     * Allows you to propagate changes to a [LegacyAddress] through the [Wallet]
     *
     * @param legacyAddress The updated address
     * @return A [Completable] object representing a successful save
     */
    @WebRequest
    internal fun updateLegacyAddress(legacyAddress: LegacyAddress): Completable =
        Completable.fromCallable {
            payloadManager.updateLegacyAddress(legacyAddress)
        }

    // /////////////////////////////////////////////////////////////////////////
    // CONTACTS/METADATA/IWCS/CRYPTO-MATRIX METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Loads previously saved nodes from the Metadata service. If none are found, the [Observable]
     * returns false.
     *
     * @return An [Observable] object wrapping a boolean value, representing successfully
     * loaded nodes
     */
    @WebRequest
    internal fun loadNodes(): Observable<Boolean> =
        Observable.fromCallable { payloadManager.loadNodes() }

    /**
     * Generates the metadata and shared metadata nodes if necessary.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    @WebRequest
    internal fun generateNodes(): Completable = Completable.fromCallable {
        payloadManager.generateNodes()
    }

    /**
     * Registers the user's MDID with the metadata service.
     *
     * @return An [Observable] wrapping a [ResponseBody]
     */
    @WebRequest
    internal fun registerMdid(): Observable<ResponseBody> =
        payloadManager.registerMdid(payloadManager.metadataNodeFactory.sharedMetadataNode)

    /**
     * Unregisters the user's MDID from the metadata service.
     *
     * @return An [Observable] wrapping a [ResponseBody]
     */
    @WebRequest
    internal fun unregisterMdid(): Observable<ResponseBody> =
        payloadManager.unregisterMdid(payloadManager.metadataNodeFactory.sharedMetadataNode)
}
