package piuk.blockchain.android.ui.balance.adapter

import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.transactions.models.Displayable

internal fun Displayable.formatting() =
    when (direction) {
        TransactionSummary.Direction.TRANSFERRED -> transferredFormatting(this)
        TransactionSummary.Direction.RECEIVED -> receivedFormatting(this)
        TransactionSummary.Direction.SENT -> sendFormatting(this)
    }

internal class DisplayableFormatting(
    @StringRes
    val text: Int,

    @ColorRes
    val directionColour: Int,

    @DrawableRes
    val valueBackground: Int
)

private fun transferredFormatting(tx: Displayable) =
    DisplayableFormatting(
        text = R.string.MOVED,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_transferred_50,
            R.drawable.rounded_view_transferred
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_gray_transferred_50,
            R.color.product_gray_transferred
        )
    )

private fun receivedFormatting(tx: Displayable) =
    DisplayableFormatting(
        text = R.string.RECEIVED,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_green_50,
            R.drawable.rounded_view_green
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_green_received_50,
            R.color.product_green_received
        )
    )

private fun sendFormatting(tx: Displayable) =
    DisplayableFormatting(
        text = R.string.SENT,
        valueBackground = getColorForConfirmations(
            tx,
            R.drawable.rounded_view_red_50,
            R.drawable.rounded_view_red
        ),
        directionColour = getColorForConfirmations(
            tx,
            R.color.product_red_sent_50,
            R.color.product_red_sent
        )
    )

private fun getColorForConfirmations(
    tx: Displayable,
    @DrawableRes colorLight: Int,
    @DrawableRes colorDark: Int
) = if (tx.confirmations < tx.cryptoCurrency.requiredConfirmations) colorLight else colorDark