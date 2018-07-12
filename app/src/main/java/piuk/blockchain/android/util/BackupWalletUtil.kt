package piuk.blockchain.android.util

import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.injection.PresenterScope
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject

@PresenterScope
class BackupWalletUtil @Inject constructor(
    private val payloadDataManager: PayloadDataManager,
    environmentConfig: EnvironmentConfig
) {

    private val networkParameters = environmentConfig.bitcoinNetworkParameters

    /**
     * Returns an ordered list of [Int], [String] pairs which can be used to confirm mnemonic.
     */
    fun getConfirmSequence(secondPassword: String?): List<Pair<Int, String>> {
        val mnemonic = getMnemonic(secondPassword)
        val randomGenerator = SecureRandom()
        val seen = mutableListOf<Int>()

        var i = 0
        while (i < 3) {
            val number = randomGenerator.nextInt(mnemonic!!.size)
            if (!seen.contains(number)) {
                seen.add(number)
                i++
            }
        }

        seen.sort()

        return (0..2).map { seen[it] to mnemonic!![seen[it]] }
    }

    /**
     * Returns a [MutableList] of Strings representing the user's backup mnemonic, or null
     * if the mnemonic isn't found.
     */
    fun getMnemonic(secondPassword: String?): List<String>? = try {
        payloadDataManager.wallet!!.decryptHDWallet(networkParameters, 0, secondPassword)
        payloadDataManager.wallet!!.hdWallets[0].mnemonic.toList()
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}
