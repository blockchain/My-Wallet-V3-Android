package com.blockchain.kycui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import piuk.blockchain.android.constants.URL_PRIVACY_POLICY
import piuk.blockchain.android.constants.URL_TOS_POLICY
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_terms_and_conditions as textViewTerms

class KycSplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val disclaimerStart = getString(R.string.kyc_splash_terms_and_conditions)
        val terms = getString(R.string.kyc_splash_terms_and_conditions_blockchain)
        val ampersand = "&"
        val privacy = getString(R.string.kyc_splash_terms_and_conditions_privacy)
        val defaultClickSpan = object : ClickableSpan() {
            override fun onClick(view: View) = Unit
            override fun updateDrawState(ds: TextPaint?) = Unit
        }
        val termsClickSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_TOS_POLICY)))
            }
        }
        val privacyClickSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACY_POLICY)))
            }
        }

        makeLinks(
            textViewTerms,
            arrayOf(disclaimerStart, terms, ampersand, privacy),
            arrayOf(defaultClickSpan, termsClickSpan, defaultClickSpan, privacyClickSpan)
        )
    }

    private fun makeLinks(
        textView: TextView,
        links: Array<String>,
        clickableSpans: Array<ClickableSpan>
    ) {
        val finalString = links.joinToString(separator = " ")
        val spannableString = SpannableString(finalString)
        for (i in links.indices) {
            val clickableSpan = clickableSpans[i]
            val link = links[i]

            val startIndexOfLink = finalString.indexOf(link)
            spannableString.setSpan(
                clickableSpan,
                startIndexOfLink,
                startIndexOfLink + link.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.apply {
            highlightColor = Color.TRANSPARENT
            movementMethod = LinkMovementMethod.getInstance()
            setText(spannableString, TextView.BufferType.SPANNABLE)
        }
    }
}