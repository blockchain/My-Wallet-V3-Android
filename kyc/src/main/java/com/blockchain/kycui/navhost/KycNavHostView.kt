package com.blockchain.kycui.navhost

import android.support.annotation.StringRes
import androidx.navigation.NavDirections
import com.blockchain.kycui.navhost.models.CampaignType
import piuk.blockchain.androidcoreui.ui.base.View

interface KycNavHostView : View {

    val campaignType: CampaignType

    fun displayLoading(loading: Boolean)

    fun showErrorToastAndFinish(@StringRes message: Int)

    fun navigate(directions: NavDirections)

    fun navigateToAirdropSplash()
}
