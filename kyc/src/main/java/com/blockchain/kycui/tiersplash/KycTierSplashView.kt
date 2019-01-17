package com.blockchain.kycui.tiersplash

import android.support.annotation.StringRes
import androidx.navigation.NavDirections
import com.blockchain.kyc.models.nabu.TiersJson

interface KycTierSplashView : piuk.blockchain.androidcoreui.ui.base.View {

    fun navigateTo(directions: NavDirections, tier: Int)

    fun showErrorToast(@StringRes message: Int)

    fun renderTiersList(tiers: TiersJson)
}
