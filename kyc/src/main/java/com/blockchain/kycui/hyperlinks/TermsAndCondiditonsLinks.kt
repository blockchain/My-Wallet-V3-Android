package com.blockchain.kycui.hyperlinks

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.annotation.StringRes
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import piuk.blockchain.android.constants.URL_PRIVACY_POLICY
import piuk.blockchain.android.constants.URL_TOS_POLICY
import piuk.blockchain.kyc.R

fun TextView.renderTermsLinks(@StringRes startText: Int) {
    val disclaimerStart = context.getString(startText) + "\n"
    val terms = context.getString(R.string.kyc_splash_terms_and_conditions_terms)
    val ampersand = "&"
    val privacy = context.getString(R.string.kyc_splash_terms_and_conditions_privacy)
    val defaultClickSpan = object : ClickableSpan() {
        override fun onClick(view: View) = Unit
        override fun updateDrawState(ds: TextPaint?) = Unit
    }
    val termsClickSpan = context.goToUrlClickableSpan(URL_TOS_POLICY)
    val privacyClickSpan = context.goToUrlClickableSpan(URL_PRIVACY_POLICY)

    formatLinks(
        disclaimerStart to defaultClickSpan,
        terms to termsClickSpan,
        ampersand to defaultClickSpan,
        privacy to privacyClickSpan
    )
}

private fun TextView.formatLinks(vararg linkPairs: Pair<String, ClickableSpan>) {
    val finalString = linkPairs.joinToString(separator = " ") { it.first }
    val spannableString = SpannableString(finalString)

    linkPairs.forEach { (link, span) ->
        val startIndexOfLink = finalString.indexOf(link)
        spannableString.setSpan(
            span,
            startIndexOfLink,
            startIndexOfLink + link.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    highlightColor = Color.TRANSPARENT
    movementMethod = LinkMovementMethod.getInstance()
    setText(spannableString, TextView.BufferType.SPANNABLE)
}

private fun Context.goToUrlClickableSpan(url: String) =
    object : ClickableSpan() {
        override fun onClick(widget: View?) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
