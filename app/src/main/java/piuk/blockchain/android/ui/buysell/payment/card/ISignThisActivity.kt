package piuk.blockchain.android.ui.buysell.payment.card

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.crashlytics.android.answers.PurchaseEvent
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.payment.complete.CoinifyPaymentCompleteActivity
import piuk.blockchain.androidcore.utils.annotations.Thunk
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.util.*
import kotlin.math.absoluteValue
import kotlinx.android.synthetic.main.activity_isignthis_payment.web_view_isignthis as webView
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general as toolBar

class ISignThisActivity : BaseAuthActivity() {

    private val redirectUrl by unsafeLazy { intent.getStringExtra(EXTRA_REDIRECT_URL) }
    private val paymentId by unsafeLazy { intent.getStringExtra(EXTRA_PAYMENT_ID) }
    private var paymentComplete = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isignthis_payment)
        setupToolbar(toolBar, R.string.buy_sell_isignthis_payment_method)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                handleUrl(url)
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(redirectUrl)
    }

    @Thunk
    fun handleUrl(url: String?) {
        Timber.d("URL loaded $url")
        if (url?.contains(TRADE_COMPLETE_PARTIAL_URL) == true && url.contains(paymentId)) {
            if (!paymentComplete) {
                paymentComplete = true
                val uri = Uri.parse(url)
                val stateString = uri.getQueryParameter("state")
                val state = PaymentState.valueOf(stateString!!)
                launchPaymentCompletePage(state)
            }
        }
    }

    private fun launchPaymentCompletePage(paymentState: PaymentState) {
        val successful = paymentState == PaymentState.SUCCESS
        val cost = intent.getDoubleExtra(EXTRA_COST, -1.0)
        val fromCurrency = intent.getStringExtra(EXTRA_FROM_CURRENCY).toUpperCase()
        Logging.logPurchase(
                PurchaseEvent().putCurrency(Currency.getInstance("BTC"))
                        .putItemPrice(cost.absoluteValue.toBigDecimal())
                        .putItemName(fromCurrency)
                        .putItemType(Logging.ITEM_TYPE_CRYPTO)
                        .putSuccess(successful)
        )

        CoinifyPaymentCompleteActivity.start(this, paymentState)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun startLogoutTimer() = Unit

    companion object {

        private const val EXTRA_REDIRECT_URL =
                "piuk.blockchain.android.ui.buysell.payment.card.EXTRA_REDIRECT_URL"
        private const val EXTRA_PAYMENT_ID =
                "piuk.blockchain.android.ui.buysell.payment.card.EXTRA_PAYMENT_ID"
        private const val EXTRA_FROM_CURRENCY =
                "piuk.blockchain.android.ui.buysell.payment.card.EXTRA_FROM_CURRENCY"
        private const val EXTRA_COST =
                "piuk.blockchain.android.ui.buysell.payment.card.EXTRA_COST"

        private const val TRADE_COMPLETE_PARTIAL_URL = "https://www.coinify.com/trade/"

        fun start(
                activity: Activity,
                redirectUrl: String,
                paymentId: String,
                fromCurrency: String,
                cost: Double
        ) {
            Intent(activity, ISignThisActivity::class.java).apply {
                putExtra(EXTRA_REDIRECT_URL, redirectUrl)
                putExtra(EXTRA_PAYMENT_ID, paymentId)
                putExtra(EXTRA_FROM_CURRENCY, fromCurrency)
                putExtra(EXTRA_COST, cost)
            }.run { activity.startActivity(this) }
        }

    }
}