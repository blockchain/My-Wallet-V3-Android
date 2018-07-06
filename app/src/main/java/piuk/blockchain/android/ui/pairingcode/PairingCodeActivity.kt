package piuk.blockchain.android.ui.pairingcode

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_pairing_code.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class PairingCodeActivity : BaseMvpActivity<PairingCodeView, PairingCodePresenter>(),
    PairingCodeView {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var pairingCodePresenter: PairingCodePresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pairing_code)

        setupToolbar(toolbar_general, R.string.pairing_code_log_in)

        pairing_first_step.text = presenter.firstStep

        button_qr_toggle.setOnClickListener { onClickQRToggle() }

        onViewReady()
    }

    override fun onSupportNavigateUp(): Boolean =
        consume { onBackPressed() }

    override fun onQrLoaded(bitmap: Bitmap) {
        tv_warning.setText(R.string.pairing_code_warning_2)
        iv_qr.visible()

        val width = resources.displayMetrics.widthPixels
        val height = width * bitmap.height / bitmap.width
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        iv_qr.setImageBitmap(scaledBitmap)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun showProgressSpinner() {
        progress_bar.visible()
    }

    override fun hideProgressSpinner() {
        progress_bar.gone()
    }

    override fun enforceFlagSecure() = true

    override fun createPresenter() = pairingCodePresenter

    override fun getView(): PairingCodeView = this

    private fun onClickQRToggle() {
        if (main_layout.visibility == View.VISIBLE) {
            // Show pairing QR
            main_layout.gone()
            button_qr_toggle.setText(R.string.pairing_code_hide_qr)
            qr_layout.visible()
            iv_qr.gone()

            presenter.generatePairingQr()
        } else {
            // Hide pairing QR
            tv_warning.setText(R.string.pairing_code_warning_1)
            main_layout.visible()
            button_qr_toggle.setText(R.string.pairing_code_show_qr)
            qr_layout.gone()
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, PairingCodeActivity::class.java)
            context.startActivity(intent)
        }
    }
}