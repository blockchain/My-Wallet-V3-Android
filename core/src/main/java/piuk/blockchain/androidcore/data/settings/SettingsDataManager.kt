package piuk.blockchain.androidcore.data.settings

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.Observable
import okhttp3.ResponseBody
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore
import piuk.blockchain.androidcore.utils.extensions.applySchedulers

class SettingsDataManager(
    private val settingsService: SettingsService,
    private val settingsDataStore: SettingsDataStore,
    rxBus: RxBus
) {
    private val rxPinning: RxPinning = RxPinning(rxBus)

    /**
     * Grabs the latest user [Settings] object from memory, or makes a web request if not
     * available.
     *
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun getSettings(): Observable<Settings> =
        rxPinning.call<Settings> { attemptFetchSettingsFromMemory() }

    /**
     * Updates the settings object by syncing it with the server. Must be called to set up the
     * [SettingsManager] class before a fetch is called.
     *
     * @param guid The user's GUID
     * @param sharedKey The shared key
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun initSettings(guid: String, sharedKey: String): Observable<Settings> {
        settingsService.initSettings(guid, sharedKey)
        return rxPinning.call<Settings> { fetchSettings() }
            .applySchedulers()
    }

    /**
     * Fetches the latest user [Settings] object from the server
     *
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun fetchSettings(): Observable<Settings> =
        rxPinning.call<Settings> { fetchSettingsFromWeb() }
            .applySchedulers()

    /**
     * Update the user's email and fetches an updated [Settings] object.
     *
     * !!! Do not call this directly, it is best to use [EmailSyncUpdater] as it handles syncing of changes with Nabu.
     *
     * @param email The email to be stored
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateEmail(email: String): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateEmail(email) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's phone number and fetches an updated [Settings] object.
     *
     * @param sms The phone number to be stored
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateSms(sms: String): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateSms(sms) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Verify the user's phone number with a verification code and fetches an updated [Settings] object.
     *
     * @param code The verification code
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun verifySms(code: String): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.verifySms(code) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's Tor blocking preference and fetches an updated [Settings] object.
     *
     * @param blocked The user's preference for blocking Tor requests
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateTor(blocked: Boolean): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateTor(blocked) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's two factor status
     *
     * @param authType The auth type being used for 2FA
     * @return An [Observable] object wrapping a [Settings] object
     * @see SettingsManager for notification types
     */
    fun updateTwoFactor(authType: Int): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateTwoFactor(authType) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's notification preferences and fetches an updated [Settings] object.
     *
     * @param notificationType The type of notification to enable
     * @param notifications An ArrayList of the currently enabled notifications
     * @return An [Observable] object wrapping a [Settings] object
     * @see SettingsManager for notification types
     */
    fun enableNotification(notificationType: Int, notifications: List<Int>): Observable<Settings> {
        return if (notifications.isEmpty() || notifications.contains(SettingsManager.NOTIFICATION_TYPE_NONE)) {
            // No notification type registered, enable
            rxPinning.call<ResponseBody> { settingsService.enableNotifications(true) }
                .flatMap { updateNotifications(notificationType) }
                .applySchedulers()
        } else if (notifications.size == 1 &&
            (notifications.contains(SettingsManager.NOTIFICATION_TYPE_EMAIL) &&
                notificationType == SettingsManager.NOTIFICATION_TYPE_SMS ||
                notifications.contains(SettingsManager.NOTIFICATION_TYPE_SMS) &&
                notificationType == SettingsManager.NOTIFICATION_TYPE_EMAIL)
        ) {
            // Contains another type already, send "All"
            rxPinning.call<ResponseBody> { settingsService.enableNotifications(true) }
                .flatMap { updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL) }
                .applySchedulers()
        } else {
            rxPinning.call<ResponseBody> { settingsService.enableNotifications(true) }
                .flatMap { fetchSettings() }
                .applySchedulers()
        }
    }

    /**
     * Update the user's notification preferences and fetches an updated [Settings] object.
     *
     * @param notificationType The type of notification to disable
     * @param notifications An ArrayList of the currently enabled notifications
     * @return An [Observable] object wrapping a [Settings] object
     * @see SettingsManager for notification types
     */
    fun disableNotification(notificationType: Int, notifications: List<Int>): Observable<Settings> {
        return if (notifications.isEmpty() || notifications.contains(SettingsManager.NOTIFICATION_TYPE_NONE)) {
            // No notifications anyway, return Settings
            rxPinning.call<Settings> { fetchSettings() }
                .applySchedulers()
        } else if (notifications.contains(SettingsManager.NOTIFICATION_TYPE_ALL) ||
            notifications.contains(SettingsManager.NOTIFICATION_TYPE_EMAIL) &&
            notifications.contains(SettingsManager.NOTIFICATION_TYPE_SMS)
        ) {
            // All types enabled, disable passed type and enable other
            updateNotifications(
                if (notificationType == SettingsManager.NOTIFICATION_TYPE_EMAIL) {
                    SettingsManager.NOTIFICATION_TYPE_SMS
                } else {
                    SettingsManager.NOTIFICATION_TYPE_EMAIL
                }
            ).applySchedulers()
        } else if (notifications.size == 1) {
            if (notifications[0] == notificationType) {
                // Remove all
                rxPinning.call<ResponseBody> { settingsService.enableNotifications(false) }
                    .flatMap { updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE) }
                    .applySchedulers()
            } else {
                // Notification type not present, no need to remove it
                rxPinning.call<Settings> { fetchSettings() }
                    .applySchedulers()
            }
        } else {
            // This should never be reached
            rxPinning.call<Settings> { fetchSettings() }
                .applySchedulers()
        }
    }

    /**
     * Updates a passed notification type and then fetches the current settings object.
     *
     * @param notificationType The notification type you wish to enable/disable
     * @return An [Observable] wrapping the Settings object
     * @see SettingsManager for notification types
     */
    private fun updateNotifications(notificationType: Int): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateNotifications(notificationType) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    private fun fetchSettingsFromWeb(): Observable<Settings> =
        Observable.defer { settingsDataStore.fetchSettings() }

    private fun attemptFetchSettingsFromMemory(): Observable<Settings> =
        Observable.defer { settingsDataStore.getSettings() }

    /**
     * Update the user's cryptoUnit unit preference and fetches an updated [Settings] object.
     *
     * @param btcUnit The user's preference for cryptoUnit unit
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateBtcUnit(btcUnit: String): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateBtcUnit(btcUnit) }
            .flatMap { fetchSettings() }
            .applySchedulers()

    /**
     * Update the user's fiat unit preference and fetches an updated [Settings] object.
     *
     * @param fiatUnit The user's preference for fiat unit
     * @return An [Observable] object wrapping a [Settings] object
     */
    fun updateFiatUnit(fiatUnit: String): Observable<Settings> =
        rxPinning.call<ResponseBody> { settingsService.updateFiatUnit(fiatUnit) }
            .flatMap { fetchSettings() }
            .applySchedulers()
}
