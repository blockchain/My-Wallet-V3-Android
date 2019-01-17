package com.blockchain.kycui.settings

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.widget.TextView
import com.blockchain.kyc.models.nabu.Kyc2TierState
import piuk.blockchain.androidcoreui.utils.extensions.applyFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont
import piuk.blockchain.kyc.R

@Suppress("unused")
class KycStatusPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private var textViewStatus: TextView? = null
    private var status2Tier = Kyc2TierState.Hidden

    init {
        init()
    }

    private var typeface: Typeface? = null

    private fun init() {
        widgetLayoutResource = R.layout.preference_identity_verification

        loadFont(
            context,
            CustomFont.MONTSERRAT_REGULAR
        ) {
            typeface = it
            // Forces setting fonts when Title is set via XMl
            this.title = title
        }
    }

    override fun setTitle(titleResId: Int) {
        title = context.getString(titleResId)
    }

    override fun setTitle(title: CharSequence?) {
        title?.let { super.setTitle(title.applyFont(typeface)) } ?: super.setTitle(title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        textViewStatus = holder!!.itemView.findViewById(R.id.text_view_preference_status)
        updateUi2Tier()
    }

    fun setKycStatus(kycState: Kyc2TierState) {
        status2Tier = kycState
        updateUi2Tier()
    }

    private fun updateUi2Tier() {
        isVisible = status2Tier != Kyc2TierState.Hidden
        textViewStatus?.apply {
            val string = when (status2Tier) {
                Kyc2TierState.Hidden -> ""
                Kyc2TierState.Locked -> context.getString(R.string.kyc_settings_tier_status_locked)
                Kyc2TierState.Tier1InReview -> context.getString(R.string.kyc_settings_tier_status_in_review, 1)
                Kyc2TierState.Tier1Approved -> context.getString(R.string.kyc_settings_tier_status_approved, 1)
                Kyc2TierState.Tier1Failed -> context.getString(R.string.kyc_settings_tier_status_failed, 1)
                Kyc2TierState.Tier2InReview -> context.getString(R.string.kyc_settings_tier_status_in_review, 2)
                Kyc2TierState.Tier2Approved -> context.getString(R.string.kyc_settings_tier_status_approved, 2)
                Kyc2TierState.Tier2Failed -> context.getString(R.string.kyc_settings_tier_status_failed, 2)
            }
            text = string
            val background = when (status2Tier) {
                Kyc2TierState.Hidden -> 0
                Kyc2TierState.Locked -> 0
                Kyc2TierState.Tier1InReview -> R.drawable.rounded_view_in_progress
                Kyc2TierState.Tier1Approved -> R.drawable.rounded_view_complete
                Kyc2TierState.Tier1Failed -> R.drawable.rounded_view_failed
                Kyc2TierState.Tier2InReview -> R.drawable.rounded_view_in_progress
                Kyc2TierState.Tier2Approved -> R.drawable.rounded_view_complete
                Kyc2TierState.Tier2Failed -> R.drawable.rounded_view_failed
            }
            val foreground = when (status2Tier) {
                Kyc2TierState.Locked -> R.color.kyc_progress_text_blue
                else -> R.color.kyc_progress_text_white
            }
            setBackgroundResource(background)
            setTextColor(ContextCompat.getColor(context, foreground))
        }
    }
}
