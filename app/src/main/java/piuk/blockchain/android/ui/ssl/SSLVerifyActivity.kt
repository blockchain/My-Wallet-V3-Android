package piuk.blockchain.android.ui.ssl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcore.data.connectivity.ConnectionEvent
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import javax.inject.Inject

class SSLVerifyActivity : BaseMvpActivity<SSLVerifyView, SSLVerifyPresenter>(), SSLVerifyView {

    @Inject
    lateinit var presenter: SSLVerifyPresenter

    private val allowRetry: Boolean by unsafeLazy {
        intent.getBooleanExtra(EXTRA_ALLOW_RETRY, false)
    }
    private val warningMessage: Int by unsafeLazy {
        intent.getIntExtra(EXTRA_WARNING, R.string.unexpected_error)
    }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        onViewReady()
    }

    override fun startLogoutTimer() {
        // No-op
    }

    override fun createPresenter() = presenter

    override fun getView() = this

    override fun showWarningPrompt() {

        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(warningMessage)
            .setCancelable(false)

        if (allowRetry) {
            builder.setPositiveButton(
                R.string.retry
            ) { _, _ -> presenter.validateSSL() }
        }

        builder.setNegativeButton(
            R.string.exit
        ) { _, _ -> finish() }

        val dialog = builder.create()
        dialog.show()
    }

    companion object {

        private const val EXTRA_ALLOW_RETRY = "piuk.blockchain.android.EXTRA_ALLOW_RETRY"
        private const val EXTRA_WARNING = "piuk.blockchain.android.EXTRA_WARNING"

        @JvmStatic
        fun start(context: Context, connectionEvent: ConnectionEvent) {

            val intent = Intent(context, SSLVerifyActivity::class.java)

            if (connectionEvent == ConnectionEvent.PINNING_FAIL) {
                // Not safe to continue
                intent.putExtra(EXTRA_ALLOW_RETRY, false)
                intent.putExtra(EXTRA_WARNING, R.string.ssl_pinning_invalid)
            } else {
                intent.putExtra(EXTRA_ALLOW_RETRY, true)
                intent.putExtra(EXTRA_WARNING, R.string.ssl_no_connection)
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}