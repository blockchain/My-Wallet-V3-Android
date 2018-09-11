package com.blockchain.kycui.status

import android.support.annotation.StringRes
import com.blockchain.kyc.models.nabu.KycState
import piuk.blockchain.androidcoreui.ui.base.View

interface KycStatusView : View {

    fun finishPage()

    fun renderUi(kycState: KycState)

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun startExchange()

    fun showToast(@StringRes message: Int)

    fun showNotificationsEnabledDialog()
}
