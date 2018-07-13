package piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_fragment_coinify_id_in_review.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.coinify.signup.CoinifyFlowListener
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject
import kotlinx.android.synthetic.main.dialog_fragment_coinify_id_in_review.text_view_finish_verification as finishKyc

class CoinifyIdentityInReviewFragment :
    BaseFragment<CoinifyIdentityInReviewView, CoinifyIdentityInReviewPresenter>(),
    CoinifyIdentityInReviewView {

    @Inject
    lateinit var presenter: CoinifyIdentityInReviewPresenter
    private var progressDialog: MaterialProgressDialog? = null
    private var signUpListener: CoinifyFlowListener? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.dialog_fragment_coinify_id_in_review)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonContinue.setOnClickListener { closeAndRestartBuySell() }

        onViewReady()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CoinifyFlowListener) {
            signUpListener = context
        } else {
            throw RuntimeException("$context must implement CoinifyFlowListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        signUpListener = null
    }

    private fun closeAndRestartBuySell() {
        signUpListener?.requestFinish()
    }

    override fun onShowLoading() {
        displayProgressDialog()
        textviewReviewMessage.invisible()
        textviewReviewStatus.invisible()
    }

    override fun dismissLoading() {
        dismissProgressDialog()
    }

    override fun onShowCompleted() {
        textviewReviewStatus.text = getString(
            R.string.buy_sell_review_status,
            getString(R.string.buy_sell_review_status_in_completed)
        )
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
    }

    override fun onShowReviewing() {
        textviewReviewStatus.text = getString(
            R.string.buy_sell_review_status,
            getString(R.string.buy_sell_review_status_in_reviewing)
        )
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
    }

    override fun onShowPending() {
        textviewReviewTitle.setText(R.string.buy_sell_review_finish_verification_title)
        textviewReviewStatus.apply {
            text = getString(
                R.string.buy_sell_review_status,
                getString(R.string.buy_sell_review_status_in_pending)
            )
            VectorDrawableCompat.create(
                resources,
                R.drawable.vector_alert,
                ContextThemeWrapper(requireActivity(), R.style.AppTheme).theme
            )?.run {
                DrawableCompat.wrap(this)
                DrawableCompat.setTint(this, getResolvedColor(R.color.primary_navy_medium))
                setCompoundDrawablesWithIntrinsicBounds(this, null, null, null)
            }
            visible()
        }

        textviewReviewMessage.visible()
        textviewReviewMessage.setText(R.string.buy_sell_review_status_pending_message)
        finishKyc.apply {
            visible()
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { signUpListener?.requestStartVerifyIdentification() }
        }
    }

    override fun onShowRejected() {
        textviewReviewStatus.text = getString(
            R.string.buy_sell_review_status,
            getString(R.string.buy_sell_review_status_in_rejected)
        )
        textviewReviewTitle.setText(R.string.buy_sell_review_failed)
        textviewReviewMessage.text = getString(R.string.buy_sell_review_status_failed)
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
    }

    override fun onShowExpired() {
        textviewReviewStatus.text = getString(
            R.string.buy_sell_review_status,
            getString(R.string.buy_sell_review_status_in_expired)
        )
        textviewReviewTitle.setText(R.string.buy_sell_review_failed)
        textviewReviewMessage.text = getString(R.string.buy_sell_review_status_failed)
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
    }

    override fun onShowFailed() {
        textviewReviewStatus.text = getString(
            R.string.buy_sell_review_status,
            getString(R.string.buy_sell_review_status_in_failed)
        )
        textviewReviewTitle.setText(R.string.buy_sell_review_failed)
        textviewReviewMessage.text = getString(R.string.buy_sell_review_status_failed)
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
    }

    override fun onShowDocumentsRequested() {
        textviewReviewStatus.text = getString(
            R.string.buy_sell_review_status,
            getString(R.string.buy_sell_review_status_in_docs_requested)
        )
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
    }

    override fun onFinish() {
        activity?.finish()
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    private fun displayProgressDialog() {
        if (activity?.isFinishing == false) {
            progressDialog = MaterialProgressDialog(context).apply {
                setMessage(getString(R.string.please_wait))
                setCancelable(false)
                show()
            }
        }
    }

    private fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    companion object {

        internal fun newInstance(): CoinifyIdentityInReviewFragment =
            CoinifyIdentityInReviewFragment()
    }
}