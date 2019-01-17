package piuk.blockchain.androidcore.data.walletoptions

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Locale

class WalletOptionsDataManager(
    authService: AuthService,
    private val walletOptionsState: WalletOptionsState,
    private val settingsDataManager: SettingsDataManager,
    private val explorerUrl: String
) {

    private val walletOptionsService by unsafeLazy {
        authService.getWalletOptions()
            .subscribeOn(Schedulers.io())
            .cache()
    }

    /**
     * ReplaySubjects will re-emit items it observed.
     * It is safe to assumed that walletOptions and
     * the user's country code won't change during an active session.
     */
    private fun initWalletOptionsReplaySubjects() {
        walletOptionsService
            .subscribeOn(Schedulers.io())
            .subscribeWith(walletOptionsState.walletOptionsSource)
    }

    @Suppress("unused")
    private fun initSettingsReplaySubjects(guid: String, sharedKey: String) {
        settingsDataManager.initSettings(guid, sharedKey)

        settingsDataManager.getSettings()
            .subscribeOn(Schedulers.io())
            .subscribeWith(walletOptionsState.walletSettingsSource)
    }

    @Suppress("unused") // May be useful in future
    fun isInUsa(): Observable<Boolean> =
        walletOptionsState.walletSettingsSource.map { it.countryCode == "US" }

    fun getCoinifyPartnerId(): Observable<Int> =
        walletOptionsState.walletOptionsSource.map { it.partners.coinify.partnerId }

    fun getBchFee(): Single<Int> = walletOptionsService
        .map { it.bchFeePerByte }
        .singleOrError()

    fun getBuyWebviewWalletLink(): String {
        initWalletOptionsReplaySubjects()
        return (walletOptionsState.walletOptionsSource.value!!.buyWebviewWalletLink
            ?: "${explorerUrl}wallet") + "/#/intermediate"
    }

    fun getComRootLink(): String {
        return walletOptionsState.walletOptionsSource.value!!.comRootLink
    }

    fun getWalletLink(): String {
        return walletOptionsState.walletOptionsSource.value!!.walletLink
    }

    /**
     * Mobile info retrieved from wallet-options.json based on wallet setting
     */
    fun fetchInfoMessage(locale: Locale): Observable<String> {
        initWalletOptionsReplaySubjects()

        return walletOptionsState.walletOptionsSource.map { options ->
            var result = ""

            options.mobileInfo.apply {
                result = getLocalisedMessage(locale, this)
            }
            return@map result
        }
    }

    /**
     * Checks to see if the client app needs to be force updated according to the wallet.options
     * JSON file. If the client is on an unsupported Android SDK, the check is bypassed to prevent
     * locking users out forever. Otherwise, an app version code ([piuk.blockchain.android.BuildConfig.VERSION_CODE])
     * less than the supplied minVersionCode will return true, and the client should be forcibly
     * upgraded.
     *
     * @param versionCode The version code of the current app
     * @param sdk The device's Android SDK version
     * @return A [Boolean] value contained within an [Observable]
     */
    fun checkForceUpgrade(versionCode: Int, sdk: Int): Observable<Boolean> {
        initWalletOptionsReplaySubjects()

        return walletOptionsState.walletOptionsSource.map {
            val androidUpgradeMap = it.androidUpgrade ?: mapOf()
            var forceUpgrade = false
            val minSdk = androidUpgradeMap["minSdk"] ?: 0
            val minVersionCode = androidUpgradeMap["minVersionCode"] ?: 0
            if (sdk < minSdk) {
                // Can safely ignore force upgrade
            } else {
                if (versionCode < minVersionCode) {
                    // Force the client to update
                    forceUpgrade = true
                }
            }

            return@map forceUpgrade
        }
    }

    fun getLocalisedMessage(locale: Locale, map: Map<String, String>): String {
        var result = ""

        if (map.isNotEmpty()) {
            val lcid = locale.language + "-" + locale.country
            val language = locale.language

            result = when {
                map.containsKey(language) -> map[language] ?: ""
                // Regional
                map.containsKey(lcid) -> map[lcid] ?: ""
                // Default
                else -> map["en"] ?: ""
            }
        }

        return result
    }

    fun getLastEthTransactionFuse(): Observable<Long> {
        return walletOptionsState.walletOptionsSource
            .map { return@map it.ethereum.lastTxFuse }
    }
}
