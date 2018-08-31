package com.blockchain.kycui.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.kyc.models.nabu.KycState
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.activity_kyc_status.button_kyc_status_next as buttonNext
import kotlinx.android.synthetic.main.activity_kyc_status.image_view_kyc_status as imageView
import kotlinx.android.synthetic.main.activity_kyc_status.text_view_verification_message as textViewMessage
import kotlinx.android.synthetic.main.activity_kyc_status.text_view_verification_state as textViewStatus
import kotlinx.android.synthetic.main.activity_kyc_status.text_view_verification_subtitle as textViewSubtitle
import kotlinx.android.synthetic.main.activity_kyc_status.toolbar_kyc as toolBar

class KycStatusActivity : BaseMvpActivity<KycStatusView, KycStatusPresenter>(), KycStatusView {

    private val presenter: KycStatusPresenter by inject()
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_status)
        setupToolbar(toolBar, R.string.kyc_splash_title)

        onViewReady()
    }

    override fun startExchange() {
        // TODO: Start the exchange activity here
        TODO("not implemented")
    }

    override fun renderUi(kycState: KycState) {
        when (kycState) {
            KycState.Pending -> onPending()
            KycState.UnderReview -> onInReview()
            KycState.Expired, KycState.Rejected -> onFailed()
            KycState.Verified -> onVerified()
            KycState.None -> throw IllegalStateException(
                "Users who haven't started KYC should not be able to access this page"
            )
        }
    }

    private fun onPending() {
        imageView.setImageDrawable(getResolvedDrawable(R.drawable.vector_in_progress))
        textViewSubtitle.visible()
        textViewStatus.setTextColor(getResolvedColor(R.color.kyc_in_progress))
        textViewStatus.setText(R.string.kyc_status_title_in_progress)
        textViewMessage.setText(R.string.kyc_status_message_in_progress)
        buttonNext.apply {
            setText(R.string.kyc_status_button_notify_me)
            setOnClickListener { presenter.onClickNotifyUser() }
            visible()
        }
    }

    private fun onInReview() {
        imageView.setImageDrawable(getResolvedDrawable(R.drawable.vector_in_progress))
        textViewStatus.setTextColor(getResolvedColor(R.color.kyc_in_progress))
        textViewStatus.setText(R.string.kyc_status_title_in_review)
        textViewMessage.setText(R.string.kyc_status_message_under_review)
        buttonNext.apply {
            setText(R.string.kyc_status_button_notify_me)
            setOnClickListener { presenter.onClickNotifyUser() }
            visible()
        }
    }

    private fun onFailed() {
        imageView.setImageDrawable(getResolvedDrawable(R.drawable.vector_failed))
        textViewStatus.setTextColor(getResolvedColor(R.color.product_red_medium))
        textViewStatus.setText(R.string.kyc_status_title_failed)
        textViewMessage.setText(R.string.kyc_status_message_failed)
        buttonNext.gone()
    }

    private fun onVerified() {
        imageView.setImageDrawable(getResolvedDrawable(R.drawable.vector_verified))
        textViewStatus.setTextColor(getResolvedColor(R.color.kyc_progress_green))
        textViewStatus.setText(R.string.kyc_settings_status_verified)
        textViewMessage.setText(R.string.kyc_status_message_verified)
        buttonNext.apply {
            setText(R.string.kyc_status_button_get_started)
            setOnClickListener { presenter.onClickContinue() }
            visible()
        }
    }

    override fun showToast(message: Int) {
        toast(message, ToastCustom.TYPE_OK)
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(this).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun finishPage() {
        toast(R.string.kyc_status_error, ToastCustom.TYPE_ERROR)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): KycStatusPresenter = presenter

    override fun getView(): KycStatusView = this

    companion object {

        @JvmStatic
        fun start(context: Context) {
            Intent(context, KycStatusActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}
