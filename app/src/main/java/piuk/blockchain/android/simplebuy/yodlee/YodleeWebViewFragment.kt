package piuk.blockchain.android.simplebuy.yodlee

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.blockchain.notifications.analytics.Analytics
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_yodlee_webview.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.SimpleBuyNavigator
import piuk.blockchain.android.simplebuy.SimpleBuyScreen
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import timber.log.Timber
import java.net.URLEncoder

class YodleeWebViewFragment : Fragment(R.layout.fragment_yodlee_webview), FastLinkInterfaceHandler.FastLinkListener,
    YodleeWebClient.YodleeWebClientInterface, SimpleBuyScreen {

    private val analytics: Analytics by inject()

    private val fastLinkUrl: String by lazy {
        arguments?.getString(FAST_LINK_URL) ?: ""
    }

    private val accessToken: String by lazy {
        arguments?.getString(ACCESS_TOKEN) ?: ""
    }

    private val configName: String by lazy {
        arguments?.getString(CONFIG_NAME) ?: ""
    }

    private val accessTokenKey = "accessToken"
    private val bearerParam: String by lazy { "Bearer $accessToken" }
    private val extraParamsKey = "extraParams"
    private val extraParamConfigName: String
        get() = "configName=$configName"
    private val extraParamEncoding = "UTF-8"

    private val yodleeQuery: String by lazy {
        Uri.Builder()
            .appendQueryParameter(accessTokenKey, bearerParam)
            .appendQueryParameter(extraParamsKey, URLEncoder.encode(extraParamConfigName, extraParamEncoding))
            .build().query ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.link_a_bank)

        setupWebView()
        yodlee_retry.setOnClickListener {
            loadYodlee()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = yodlee_webview.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        yodlee_webview.webViewClient = YodleeWebClient(this)
        yodlee_webview.addJavascriptInterface(FastLinkInterfaceHandler(this), "YWebViewHandler")
    }

    override fun onResume() {
        super.onResume()
        loadYodlee()
    }

    private fun loadYodlee() {
        requireActivity().runOnUiThread {
            updateViewsVisibility(true)
            yodlee_webview.clearCache(true)
            yodlee_status_label.text = getString(R.string.yodlee_connection_title)
            yodlee_subtitle.text = getString(R.string.yodlee_connection_subtitle)
            yodlee_webview.gone()
            yodlee_retry.gone()
            yodlee_webview.postUrl(fastLinkUrl, yodleeQuery.toByteArray())
        }
    }

    override fun flowSuccess(providerAccountId: String, accountId: String) {
        analytics.logEvent(SimpleBuyAnalytics.ACH_SUCCESS)
        navigator().launchBankLinking(accountProviderId = providerAccountId, accountId = accountId)
    }

    override fun flowError(error: FastLinkInterfaceHandler.FastLinkFlowError, reason: String?) {
        requireActivity().runOnUiThread {
            showError(getString(R.string.yodlee_parsing_error), reason)
        }
    }

    private fun showError(errorText: String, reason: String?) {
        yodlee_webview.gone()
        yodlee_icon.gone()
        yodlee_progress.gone()
        yodlee_status_label.text = errorText
        yodlee_status_label.visible()

        yodlee_retry.visible()
        yodlee_retry.setOnClickListener { loadYodlee() }
        reason?.let {
            yodlee_subtitle.visible()
            yodlee_subtitle.text = it
        } ?: kotlin.run {
            yodlee_subtitle.gone()
        }
    }

    override fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        requireContext().startActivity(intent)
    }

    override fun pageFinishedLoading() {
        yodlee_webview.visible()
        updateViewsVisibility(false)
    }

    private fun updateViewsVisibility(visible: Boolean) {
        yodlee_progress.visibleIf { visible }
        yodlee_status_label.visibleIf { visible }
        yodlee_subtitle.visibleIf { visible }
        yodlee_icon.visibleIf { visible }
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator)
            ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true

    companion object {
        private const val FAST_LINK_URL: String = "FAST_LINK_URL"
        private const val ACCESS_TOKEN: String = "ACCESS_TOKEN"
        private const val CONFIG_NAME: String = "CONFIG_NAME"

        fun newInstance(
            fastLinkUrl: String,
            accessToken: String,
            configName: String
        ): YodleeWebViewFragment = YodleeWebViewFragment().apply {
            arguments = Bundle().apply {
                putString(FAST_LINK_URL, fastLinkUrl)
                putString(ACCESS_TOKEN, accessToken)
                putString(CONFIG_NAME, configName)
            }
        }
    }
}

class YodleeWebClient(private val listener: YodleeWebClientInterface) : WebViewClient() {
    interface YodleeWebClientInterface {
        fun pageFinishedLoading()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (BuildConfig.DEBUG) {
            Timber.e("Yodlee SSL error: $error")
            handler?.proceed()
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        listener.pageFinishedLoading()
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        view.loadUrl(url)
        return true
    }
}

class FastLinkInterfaceHandler(private val listener: FastLinkListener) {

    interface FastLinkListener {
        fun flowSuccess(providerAccountId: String, accountId: String)
        fun flowError(error: FastLinkFlowError, reason: String? = null)
        fun openExternalUrl(url: String)
    }

    enum class FastLinkFlowError {
        JSON_PARSING,
        FLOW_QUIT_BY_USER,
        OTHER
    }

    @JavascriptInterface
    fun postMessage(data: String?) {
        if (data == null) return
        if (!data.isValidJSONObject()) {
            return
        }
        val message = Gson().fromJson(data, FastLinkMessage::class.java)
        val messageType = message.type ?: return
        val messageData = message.data ?: return

        if (messageType.equals(POST_MESSAGE, true) && messageData.action != null) {
            handlePostMessage(messageData)
        } else if (messageType.equals(OPEN_EXTERNAL_URL, true)) {
            messageData.externalUrl?.let {
                listener.openExternalUrl(it)
            }
        }
    }

    private fun handlePostMessage(data: MessageData) {
        data.action?.let {
            if (it.equals(EXIT_ACTION, true)) {
                handleExitAction(data)
            }
        }
    }

    private fun handleExitAction(data: MessageData) {
        if (data.sites?.isNotEmpty() == true && data.sites[0].status.equals(FLOW_SUCCESS, true)) {
            handleSitesSuccess(data.sites[0])
        } else if (data.status != null) {
            handleMessageStatus(data.status, data.reason)
        } else {
            listener.flowError(FastLinkFlowError.OTHER)
        }
    }

    private fun handleSitesSuccess(siteData: SiteData) {
        val accountId = siteData.accountId ?: kotlin.run {
            listener.flowError(FastLinkFlowError.OTHER)
            return
        }
        val providerAccountId: String = siteData.providerAccountId ?: kotlin.run {
            listener.flowError(FastLinkFlowError.OTHER)
            return
        }
        listener.flowSuccess(providerAccountId = providerAccountId, accountId = accountId)
    }

    private fun handleMessageStatus(status: String, reason: String?) {
        if (
            status.equals(FLOW_ABANDONED, true) ||
            status.equals(USER_CLOSE_ACTION, true)
        ) {
            listener.flowError(FastLinkFlowError.FLOW_QUIT_BY_USER, reason)
        } else
            listener.flowError(FastLinkFlowError.OTHER)
    }

    private fun String.isValidJSONObject(): Boolean {
        try {
            JSONObject(this)
        } catch (ex: JSONException) {
            return false
        }
        return true
    }

    companion object {
        // Message types
        private const val POST_MESSAGE = "POST_MESSAGE"
        private const val OPEN_EXTERNAL_URL = "OPEN_EXTERNAL_URL"

        // Handled actions
        private const val EXIT_ACTION = "exit"

        // Data statuses
        private const val FLOW_SUCCESS = "SUCCESS"
        private const val FLOW_ABANDONED = "ACTION_ABANDONED"
        private const val USER_CLOSE_ACTION = "USER_CLOSE_ACTION"
    }
}