package com.blockchain.kycui.hyperlinks

import android.graphics.Color
import android.support.annotation.StringRes
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

/**
 * Sets the text in the receiver to the [text], and inserts [link] at %1$s position.
 * If that does not correctly insert, it sets the text to just [link].
 * It makes [link] a hyperlink which executes [action] on click.
 */
fun TextView.insertSingleLink(
    @StringRes text: Int,
    @StringRes link: Int,
    action: () -> Unit
) {
    val linkString = context.getString(link)
    val body = context.getString(text, linkString)
    val startIndex = body.indexOf(linkString)
    val finalString = if (startIndex == -1) linkString else body
    setText(
        SpannableString(finalString)
            .apply {
                setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View?) {
                            action()
                        }
                    },
                    startIndex,
                    startIndex + linkString.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            },
        TextView.BufferType.SPANNABLE
    )
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}
