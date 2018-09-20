package com.blockchain.morph.ui.homebrew.exchange.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.view.View
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.extensions.toDrawable
import com.blockchain.morph.ui.homebrew.exchange.extensions.toStatusString
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import kotlinx.android.synthetic.main.activity_homebrew_trade_detail.*
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible

class HomebrewTradeDetailActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_detail)

        val trade = intent.extras.get("EXTRA_TRADE") as Trade
        setupToolbar(R.id.toolbar_general, R.string.order_detail)

        status.text = trade.state.toStatusString(this)
        status.setCompoundDrawablesWithIntrinsicBounds(
            trade.state.toDrawable(this),
            null,
            null,
            null
        )
        value.text = if (trade.approximateValue()) trade.price.displayAsApproximate() else trade.price
        receive.text = trade.quantity
        fees.text = trade.fee

        if (trade.shouldStrike()) {
            receive.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }

        if (trade.refunding()) {
            receive.text = trade.quantity.displayAsApproximate()
            fees.text = trade.fee.displayAsApproximate()
            receive_title.setText(R.string.morph_status_refund_in_progress)
        }

        if (trade.refunded()) {
            receive.text = trade.quantity
            fees.text = trade.fee.displayAsNegative()
            receive_title.setText(R.string.morph_exchange)
        }

        trade_id.text = trade.id
        exchange.text = trade.depositQuantity
        status_detail_textView.text = trade.toMessage()

        if (trade.expired()) {
            button_request_refund.visible()
            button_request_refund.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REFUND_LINK)))
            }
        }

        var idClicked = false
        val transition = AutoTransition().apply { duration = 300 }

        trade_id.setOnClickListener {
            if (!idClicked) {
                idClicked = true
                copyToClipboard(trade)
                trade_id_title.invisible()
                TransitionManager.beginDelayedTransition(constraint_layout_morph_detail, transition)

                ConstraintSet().apply {
                    clone(constraint_layout_morph_detail)
                    connect(
                        trade_id.id,
                        ConstraintSet.START,
                        constraint_layout_morph_detail.id,
                        ConstraintSet.START
                    )
                    applyTo(constraint_layout_morph_detail)
                }
            } else {
                idClicked = false
                TransitionManager.beginDelayedTransition(constraint_layout_morph_detail, transition)

                ConstraintSet().apply {
                    clone(constraint_layout_morph_detail)
                    connect(
                        trade_id.id,
                        ConstraintSet.START,
                        trade_id_title.id,
                        ConstraintSet.END
                    )
                    setVisibility(trade_id_title.id, View.VISIBLE)
                    applyTo(constraint_layout_morph_detail)
                }
            }
        }
    }

    private fun copyToClipboard(trade: Trade) {
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Trade ID", trade.id)
        clipboard.primaryClip = clip
        toast(R.string.copied_to_clipboard, ToastCustom.TYPE_OK)
    }

    private fun Trade.toMessage(): String? =
        when (this.state) {
            MorphTrade.Status.FAILED,
            MorphTrade.Status.REFUNDED -> getString(R.string.status_failed_detail)
            MorphTrade.Status.IN_PROGRESS -> getString(R.string.status_inprogress_detail)
            MorphTrade.Status.REFUND_IN_PROGRESS -> getString(R.string.status_refund_in_progress_detail)
            MorphTrade.Status.EXPIRED -> getString(R.string.status_expired_detail)
            else -> null
        }

    private fun Trade.shouldStrike(): Boolean =
        when (this.state) {
            MorphTrade.Status.EXPIRED,
            MorphTrade.Status.REFUNDED,
            MorphTrade.Status.REFUND_IN_PROGRESS,
            MorphTrade.Status.FAILED -> true
            else -> false
        }

    private fun Trade.approximateValue(): Boolean =
        this.state == MorphTrade.Status.IN_PROGRESS

    private fun Trade.refunding(): Boolean =
        this.state == MorphTrade.Status.REFUND_IN_PROGRESS

    private fun Trade.refunded(): Boolean =
        this.state == MorphTrade.Status.REFUNDED

    private fun Trade.expired(): Boolean =
        this.state == MorphTrade.Status.EXPIRED

    private fun String.displayAsApproximate() = "~$this"

    private fun String.displayAsNegative() = "-$this"

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    companion object {
        private const val REFUND_LINK =
            "https://support.blockchain.com/hc/en-us/requests/new?ticket_form_id=360000014686"
    }
}
