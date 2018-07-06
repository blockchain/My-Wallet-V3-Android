package piuk.blockchain.android.ui.shapeshift.inprogress

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_trade_in_progress.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.shapeshift.models.TradeProgressUiState
import piuk.blockchain.android.ui.shapeshift.overview.ShapeShiftActivity
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import javax.inject.Inject

class TradeInProgressActivity : BaseMvpActivity<TradeInProgressView, TradeInProgressPresenter>(),
    TradeInProgressView {

    @Suppress("MemberVisibilityCanPrivate", "unused")
    @Inject
    lateinit var tradeInProgressPresenter: TradeInProgressPresenter

    override val depositAddress: String by lazy { intent.getStringExtra(EXTRA_DEPOSIT_ADDRESS) }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_in_progress)
        setupToolbar(toolbar_general, R.string.shapeshift_sending_title)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        button_close.setOnClickListener {
            // Take the user back to the overview page but maintain the back stack
            val intent = Intent(this, MainActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            startActivity(intent)
            ShapeShiftActivity.start(this)
        }

        onViewReady()
    }

    override fun onBackPressed() = Unit

    @SuppressLint("StringFormatMatches")
    override fun updateUi(uiState: TradeProgressUiState) {
        setupToolbar(toolbar_general, uiState.title)
        textview_sending_message.setText(uiState.message)
        imageview_progress.setImageDrawable(ContextCompat.getDrawable(this, uiState.icon))
        textview_current_word_step.text =
            getString(R.string.shapeshift_step_number, uiState.stepNumber)
        textview_current_word_step.visibility =
            if (uiState.showSteps) View.VISIBLE else View.INVISIBLE
    }

    override fun createPresenter() = tradeInProgressPresenter

    override fun getView() = this

    companion object {

        private const val EXTRA_DEPOSIT_ADDRESS = "piuk.blockchain.android.EXTRA_DEPOSIT_ADDRESS"

        fun start(context: Context, depositAddress: String) {
            val intent = Intent(context, TradeInProgressActivity::class.java).apply {
                putExtra(EXTRA_DEPOSIT_ADDRESS, depositAddress)
            }
            context.startActivity(intent)
        }
    }
}