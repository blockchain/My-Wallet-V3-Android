package piuk.blockchain.androidcore.data.auth

import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.data.Status
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.ResponseBody
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.utils.annotations.Mockable
import retrofit2.Response
import javax.inject.Inject

@Mockable
class AuthService @Inject constructor(private val walletApi: WalletApi, rxBus: RxBus) {

    private val rxPinning: RxPinning = RxPinning(rxBus)

    /**
     * Returns a [WalletOptions] object, which amongst other things contains information
     * needed for determining buy/sell regions.
     */
    fun getWalletOptions(): Observable<WalletOptions> =
        rxPinning.call<WalletOptions> { walletApi.walletOptions }

    /**
     * Get encrypted copy of Payload
     *
     * @param guid A user's GUID
     * @param sessionId The session ID, retrieved from [.getSessionId]
     * @return [<] wrapping an encrypted Payload
     */
    fun getEncryptedPayload(
        guid: String,
        sessionId: String
    ): Observable<Response<ResponseBody>> = rxPinning.call<Response<ResponseBody>> {
        walletApi.fetchEncryptedPayload(guid, sessionId)
    }

    /**
     * Posts a user's 2FA code to the server. Will return an encrypted copy of the Payload if
     * successful.
     *
     * @param sessionId The current session ID
     * @param guid The user's GUID
     * @param twoFactorCode The user's generated (or received) 2FA code
     * @return An [Observable] which may contain an encrypted Payload
     */
    fun submitTwoFactorCode(
        sessionId: String,
        guid: String,
        twoFactorCode: String
    ): Observable<ResponseBody> = rxPinning.call<ResponseBody> {
        walletApi.submitTwoFactorCode(sessionId, guid, twoFactorCode)
    }

    /**
     * Gets a session ID from the server
     *
     * @param guid A user's GUID
     * @return An [Observable] wrapping a [String] response
     */
    fun getSessionId(guid: String): Observable<String> = rxPinning.call<String> {
        walletApi.getSessionId(guid)
            .map { responseBodyResponse ->
                val headers = responseBodyResponse.headers().get("Set-Cookie")
                if (headers != null) {
                    val fields = headers.split(";\\s*".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    for (field in fields) {
                        if (field.startsWith("SID=")) {
                            return@map field.substring(4)
                        }
                    }
                } else {
                    throw ApiException("Session ID not found in headers")
                }
                ""
            }
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid A user's GUID
     * @return An [Observable] wrapping the pairing encryption password
     */
    fun getPairingEncryptionPassword(guid: String): Observable<ResponseBody> =
        rxPinning.call<ResponseBody> {
            walletApi.fetchPairingEncryptionPassword(guid)
        }

    /**
     * Sends the access key to the server
     *
     * @param key The PIN identifier
     * @param value The value, randomly generated
     * @param pin The user's PIN
     * @return An [Observable] where the boolean represents success
     */
    fun setAccessKey(
        key: String,
        value: String,
        pin: String
    ): Observable<Response<Status>> =
        rxPinning.call<Response<Status>> { walletApi.setAccess(key, value, pin) }

    /**
     * Validates a user's PIN with the server
     *
     * @param key The PIN identifier
     * @param pin The user's PIN
     * @return A [Response] which may or may not contain the field "success"
     */
    fun validateAccess(key: String, pin: String): Observable<Response<Status>> =
        rxPinning.call<Response<Status>> {
            walletApi.validateAccess(key, pin)
                .doOnError {
                    if (it.message?.contains("Incorrect PIN") == true) {
                        throw InvalidCredentialsException("Incorrect PIN")
                    }
                }
        }

    /**
     * Returns a signed JWT for use with the buy/sell APIs.
     *
     * @return A [String] representing a signed JWT.
     */
    fun getSignedJwt(guid: String, sharedKey: String, partner: String): Single<String> =
        rxPinning.callSingle {
            walletApi.getSignedJsonToken(guid, sharedKey, partner)
        }
}
