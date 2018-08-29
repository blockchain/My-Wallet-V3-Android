package com.blockchain.kycui.settings

import android.content.Context
import android.graphics.Typeface
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.widget.TextView
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
    private var status = SettingsKycState.Hidden

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
        updateUi()
    }

    fun setKycStatus(kycState: SettingsKycState) {
        status = kycState
        updateUi()
    }

    private fun updateUi() {
        isVisible = status != SettingsKycState.Hidden
        when (status) {
            SettingsKycState.Unverified -> onUnverified()
            SettingsKycState.Verified -> onVerified()
            SettingsKycState.InProgress -> onInProgress()
            SettingsKycState.Failed -> onFailed()
            SettingsKycState.Hidden -> Unit
        }
    }

    private fun onUnverified() {
        textViewStatus?.apply {
            setText(R.string.kyc_settings_status_none)
            setBackgroundResource(R.drawable.rounded_view_failed)
        }
    }

    private fun onInProgress() {
        textViewStatus?.apply {
            setText(R.string.kyc_settings_status_pending)
            setBackgroundResource(R.drawable.rounded_view_in_progress)
        }
    }

    private fun onFailed() {
        textViewStatus?.apply {
            setText(R.string.kyc_settings_status_rejected)
            setBackgroundResource(R.drawable.rounded_view_failed)
        }
    }

    private fun onVerified() {
        textViewStatus?.apply {
            setText(R.string.kyc_settings_status_verified)
            setBackgroundResource(R.drawable.rounded_view_complete)
        }
    }
}
