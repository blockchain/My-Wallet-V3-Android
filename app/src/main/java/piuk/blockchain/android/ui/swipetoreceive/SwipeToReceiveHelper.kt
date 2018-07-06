package piuk.blockchain.android.ui.swipetoreceive

import info.blockchain.api.data.Balance
import io.reactivex.Observable
import io.reactivex.Single
import org.bitcoinj.core.Address
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.annotations.Mockable
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

@Mockable
@PresenterScope
class SwipeToReceiveHelper @Inject constructor(
    private val payloadDataManager: PayloadDataManager,
    private val prefsUtil: PrefsUtil,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val stringUtils: StringUtils,
    private val environmentSettings: EnvironmentConfig
) {

    /**
     * Derives 5 addresses from the current point on the receive chain. Stores them alongside the
     * account name in SharedPrefs. Only stores addresses if enabled in SharedPrefs. This should be
     * called on a Computation thread as it can take up to 2 seconds on a mid-range device.
     */
    fun updateAndStoreBitcoinAddresses() {
        if (getIfSwipeEnabled()) {
            val numOfAddresses = 5

            val defaultAccount = payloadDataManager.defaultAccount
            val receiveAccountName = defaultAccount.label
            storeBtcAccountName(receiveAccountName)

            val stringBuilder = StringBuilder()

            for (i in 0 until numOfAddresses) {
                val receiveAddress =
                    payloadDataManager.getReceiveAddressAtPosition(defaultAccount, i)
                    // Likely not initialized yet
                    ?: break

                stringBuilder.append(receiveAddress)
                    .append(",")
            }

            storeBitcoinAddresses(stringBuilder.toString())
        }
    }

    /**
     * Derives 5 addresses from the current point on the receive chain. Stores them alongside the
     * account name in SharedPrefs. Only stores addresses if enabled in SharedPrefs. This should be
     * called on a Computation thread as it can take up to 2 seconds on a mid-range device.
     */
    fun updateAndStoreBitcoinCashAddresses() {
        if (getIfSwipeEnabled()) {
            val numOfAddresses = 5

            val defaultAccount = bchDataManager.getDefaultGenericMetadataAccount()!!
            val defaultAccountPosition = bchDataManager.getDefaultAccountPosition()
            val receiveAccountName = defaultAccount.label
            storeBchAccountName(receiveAccountName)

            val stringBuilder = StringBuilder()

            for (i in 0 until numOfAddresses) {
                val receiveAddress =
                    bchDataManager.getReceiveAddressAtPosition(defaultAccountPosition, i)
                    // Likely not initialized yet
                    ?: break

                stringBuilder.append(receiveAddress).append(",")
            }

            storeBitcoinCashAddresses(stringBuilder.toString())
        }
    }

    /**
     * Stores the user's ETH address locally in SharedPrefs. Only stores addresses if enabled in
     * SharedPrefs.
     */
    fun storeEthAddress() {
        if (getIfSwipeEnabled()) {
            val ethAccount = ethDataManager.getEthWallet()?.account?.address
            if (ethAccount != null) {
                storeEthAddress(ethAccount)
            } else {
                Timber.e("ETH Wallet was null when attempting to store ETH address")
            }
        }
    }

    /**
     * Returns the next unused Bitcoin address from the list of 5 stored for swipe to receive. Can
     * return an empty String if no unused addresses are found.
     */
    fun getNextAvailableBitcoinAddressSingle(): Single<String> {
        return getBalanceOfAddresses(getBitcoinReceiveAddresses())
            .map { map ->
                for ((address, value) in map) {
                    val balance = value.finalBalance
                    if (balance.compareTo(BigInteger.ZERO) == 0) {
                        return@map address
                    }
                }
                return@map ""
            }.singleOrError()
    }

    /**
     * Returns the next unused Bitcoin Cash address from the list of 5 stored for swipe to receive.
     * Can return an empty String if no unused addresses are found.
     */
    fun getNextAvailableBitcoinCashAddressSingle(): Single<String> {
        return getBalanceOfBchAddresses(getBitcoinCashReceiveAddresses())
            .map { map ->
                for ((address, value) in map) {
                    val balance = value.finalBalance
                    if (balance.compareTo(BigInteger.ZERO) == 0) {
                        return@map Address.fromBase58(
                            environmentSettings.bitcoinCashNetworkParameters,
                            address
                        ).toCashAddress()
                    }
                }
                return@map ""
            }.singleOrError()
    }

    /**
     * Returns a List of the next 5 available unused (at the time of storage) receive addresses. Can
     * return an empty list.
     */
    fun getBitcoinReceiveAddresses(): List<String> {
        val addressString = prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")
        return when {
            addressString.isEmpty() -> emptyList()
            else -> addressString.split(",").dropLastWhile { it.isEmpty() }
        }
    }

    /**
     * Returns a List of the next 5 available unused (at the time of storage) receive addresses for
     * Bitcoin Cash. Can return an empty list.
     */
    fun getBitcoinCashReceiveAddresses(): List<String> {
        val addressString = prefsUtil.getValue(KEY_SWIPE_RECEIVE_BCH_ADDRESSES, "")
        return when {
            addressString.isEmpty() -> emptyList()
            else -> addressString.split(",").dropLastWhile { it.isEmpty() }
        }
    }

    /**
     * Returns the previously stored Ethereum address, wrapped in a [Single].
     */
    fun getEthReceiveAddressSingle(): Single<String> = Single.just(getEthReceiveAddress())

    /**
     * Returns the previously stored Ethereum address or null if not stored
     */
    fun getEthReceiveAddress(): String? =
        prefsUtil.getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null)

    /**
     * Returns the Bitcoin account name associated with the receive addresses.
     */
    fun getBitcoinAccountName(): String = prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "")

    /**
     * Returns the Bitcoin Cash account name associated with the receive addresses.
     */
    fun getBitcoinCashAccountName(): String = prefsUtil.getValue(
        KEY_SWIPE_RECEIVE_BCH_ACCOUNT_NAME,
        stringUtils.getString(R.string.bch_default_account_label)
    )

    /**
     * Returns the default account name for new Ethereum accounts.
     */
    fun getEthAccountName(): String = stringUtils.getString(R.string.eth_default_account_label)

    private fun getIfSwipeEnabled(): Boolean =
        prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)

    private fun getBalanceOfAddresses(addresses: List<String>): Observable<LinkedHashMap<String, Balance>> =
        payloadDataManager.getBalanceOfAddresses(addresses)
            .applySchedulers()

    private fun getBalanceOfBchAddresses(addresses: List<String>): Observable<LinkedHashMap<String, Balance>> =
        payloadDataManager.getBalanceOfBchAddresses(addresses)
            .applySchedulers()

    private fun storeBitcoinAddresses(addresses: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ADDRESSES, addresses)
    }

    private fun storeBitcoinCashAddresses(addresses: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_BCH_ADDRESSES, addresses)
    }

    private fun storeEthAddress(address: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, address)
    }

    private fun storeBtcAccountName(accountName: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, accountName)
    }

    private fun storeBchAccountName(accountName: String) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_BCH_ACCOUNT_NAME, accountName)
    }

    companion object {
        const val KEY_SWIPE_RECEIVE_ADDRESSES = "swipe_receive_addresses"
        const val KEY_SWIPE_RECEIVE_ETH_ADDRESS = "swipe_receive_eth_address"
        const val KEY_SWIPE_RECEIVE_BCH_ADDRESSES = "swipe_receive_bch_addresses"
        const val KEY_SWIPE_RECEIVE_ACCOUNT_NAME = "swipe_receive_account_name"
        const val KEY_SWIPE_RECEIVE_BCH_ACCOUNT_NAME = "swipe_receive_bch_account_name"
    }
}
