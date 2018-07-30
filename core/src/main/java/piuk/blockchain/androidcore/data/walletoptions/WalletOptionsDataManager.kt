package piuk.blockchain.androidcore.data.walletoptions

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@PresenterScope
class WalletOptionsDataManager @Inject constructor(
    authService: AuthService,
    private val walletOptionsState: WalletOptionsState,
    private val settingsDataManager: SettingsDataManager,
    @Named("explorer-url") private val explorerUrl: String
) {

    private val walletOptionsService by unsafeLazy {
        authService.getWalletOptions()
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

    private fun initSettingsReplaySubjects(guid: String, sharedKey: String) {
        settingsDataManager.initSettings(guid, sharedKey)

        settingsDataManager.getSettings()
            .subscribeOn(Schedulers.io())
            .subscribeWith(walletOptionsState.walletSettingsSource)
    }

    fun showShapeshift(guid: String, sharedKey: String): Observable<Boolean> {
        initWalletOptionsReplaySubjects()
        initSettingsReplaySubjects(guid, sharedKey)

        return Observable.zip(walletOptionsState.walletOptionsSource,
            walletOptionsState.walletSettingsSource,
            BiFunction { options, settings ->
                isShapeshiftAllowed(options, settings)
            }
        )
    }

    private fun isShapeshiftAllowed(options: WalletOptions, settings: Settings): Boolean {
        val isShapeShiftAllowed = options.androidFlags.let { it?.get(SHOW_SHAPESHIFT) ?: false }
        val blacklistedCountry = options.shapeshift.countriesBlacklist.let {
            it?.contains(settings.countryCode) ?: false
        }

        return isShapeShiftAllowed && !blacklistedCountry
    }

    fun isInUsa(): Observable<Boolean> =
        walletOptionsState.walletSettingsSource.map { it.countryCode == "US" }

    fun isStateWhitelisted(state: String): Observable<Boolean> =
        walletOptionsState.walletOptionsSource
            .map { it.shapeshift.statesWhitelist.let { it?.contains(state) ?: true } }

    fun getCoinifyPartnerId(): Observable<Int> =
        walletOptionsState.walletOptionsSource.map { it.partners.coinify.partnerId }

    fun getBchFee(): Int = walletOptionsState.walletOptionsSource.value!!.bchFeePerByte

    fun getShapeShiftLimit(): Int =
        walletOptionsState.walletOptionsSource.value!!.shapeshift.upperLimit

    fun getBuyWebviewWalletLink(): String {
        initWalletOptionsReplaySubjects()
        return (walletOptionsState.walletOptionsSource.value!!.buyWebviewWalletLink
            ?: explorerUrl + "wallet") + "/#/intermediate"
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

    companion object {
        private const val SHOW_SHAPESHIFT = "showShapeshift"
    }
}
