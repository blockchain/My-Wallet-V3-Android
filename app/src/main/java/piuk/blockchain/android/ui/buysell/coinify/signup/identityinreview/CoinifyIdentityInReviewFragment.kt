package piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_fragment_coinify_id_in_review.buttonContinue
import kotlinx.android.synthetic.main.dialog_fragment_coinify_id_in_review.textviewReviewMessage
import kotlinx.android.synthetic.main.dialog_fragment_coinify_id_in_review.textviewReviewStatus
import kotlinx.android.synthetic.main.dialog_fragment_coinify_id_in_review.textviewReviewTitle
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.coinify.signup.CoinifyFlowListener
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

class CoinifyIdentityInReviewFragment :
    BaseFragment<CoinifyIdentityInReviewView, CoinifyIdentityInReviewPresenter>(),
    CoinifyIdentityInReviewView {

    @Inject lateinit var presenter: CoinifyIdentityInReviewPresenter
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
        textviewReviewStatus.text = getString(
                R.string.buy_sell_review_status,
                getString(R.string.buy_sell_review_status_in_pending)
        )
        textviewReviewMessage.visible()
        textviewReviewStatus.visible()
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