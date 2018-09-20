package com.blockchain.morph.ui.homebrew.exchange.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.ui.R
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

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

internal fun MorphTrade.Status.toDrawable(context: Context): Drawable? =
    when (this) {
        MorphTrade.Status.IN_PROGRESS,
        MorphTrade.Status.NO_DEPOSITS,
        MorphTrade.Status.RECEIVED -> context.getResolvedDrawable(R.drawable.trade_status_in_progress_circle)
        MorphTrade.Status.EXPIRED,
        MorphTrade.Status.UNKNOWN -> context.getResolvedDrawable(R.drawable.trade_status_expired_circle)
        MorphTrade.Status.RESOLVED,
        MorphTrade.Status.COMPLETE -> context.getResolvedDrawable(R.drawable.trade_status_completed_circle)
        MorphTrade.Status.FAILED -> context.getResolvedDrawable(R.drawable.trade_status_failed_circle)
        MorphTrade.Status.REFUNDED -> context.getResolvedDrawable(R.drawable.trade_status_refunded_circle)
        MorphTrade.Status.REFUND_IN_PROGRESS ->
            context.getResolvedDrawable(R.drawable.trade_status_refund_in_progress_circle)
    }