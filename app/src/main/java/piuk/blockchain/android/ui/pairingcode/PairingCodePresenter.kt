package piuk.blockchain.android.ui.pairingcode

import android.graphics.Bitmap
import io.reactivex.Observable
import okhttp3.ResponseBody
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.PairingEvent
import piuk.blockchain.androidcoreui.utils.logging.PairingMethod
import javax.inject.Inject

class PairingCodePresenter @Inject constructor(
    private val qrCodeDataManager: QrCodeDataManager,
    stringUtils: StringUtils,
    private val payloadDataManager: PayloadDataManager,
    private val authDataManager: AuthDataManager
) : BasePresenter<PairingCodeView>() {

    override fun onViewReady() {
        // No op
    }

    internal val firstStep =
        String.format(stringUtils.getString(R.string.pairing_code_instruction_1), WEB_WALLET_URL)

    internal fun generatePairingQr() {
        pairingEncryptionPasswordObservable
            .doOnSubscribe { view.showProgressSpinner() }
            .doAfterTerminate { view.hideProgressSpinner() }
            .flatMap { encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()) }
            .addToCompositeDisposable(this)
            .subscribe(
                { bitmap ->
                    view.onQrLoaded(bitmap)
                    Logging.logCustom(
                        PairingEvent()
                            .putMethod(PairingMethod.REVERSE)
                    )
                },
                { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    private val pairingEncryptionPasswordObservable: Observable<ResponseBody>
        get() {
            val guid = payloadDataManager.wallet!!.guid
            return authDataManager.getPairingEncryptionPassword(guid)
        }

    private fun generatePairingCodeObservable(encryptionPhrase: String): Observable<Bitmap> {
        val guid = payloadDataManager.wallet!!.guid
        val sharedKey = payloadDataManager.wallet!!.sharedKey
        val password = payloadDataManager.tempPassword

        return qrCodeDataManager.generatePairingCode(
            guid,
            password,
            sharedKey,
            encryptionPhrase,
            600
        )
    }

    companion object {
        private const val WEB_WALLET_URL = "login.blockchain.com"
    }
}
