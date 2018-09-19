package com.blockchain.morph.ui.homebrew.exchange.extensions

import android.content.Context
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.ui.R

internal fun MorphTrade.Status.toStatusString(context: Context): String =
    when (this) {
        MorphTrade.Status.IN_PROGRESS,
        MorphTrade.Status.NO_DEPOSITS -> context.getString(R.string.morph_status_in_progress)
        MorphTrade.Status.UNKNOWN -> context.getString(R.string.morph_status_refund_unknown)
        MorphTrade.Status.RECEIVED -> context.getString(R.string.morph_status_refund_received)
        MorphTrade.Status.RESOLVED -> context.getString(R.string.morph_status_refund_resolved)
        MorphTrade.Status.COMPLETE -> context.getString(R.string.morph_status_complete)
        MorphTrade.Status.FAILED -> context.getString(R.string.morph_status_failed)
        MorphTrade.Status.REFUNDED -> context.getString(R.string.morph_status_refunded)
        MorphTrade.Status.REFUND_IN_PROGRESS -> context.getString(R.string.morph_status_refund_in_progress)
        MorphTrade.Status.EXPIRED -> context.getString(R.string.morph_status_expired)
    }