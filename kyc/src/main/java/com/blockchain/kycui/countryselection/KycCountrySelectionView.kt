package com.blockchain.kycui.countryselection

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View

interface KycCountrySelectionView : View {

    fun showProgress()

    fun hideProgress()

    fun continueFlow()

    fun invalidCountry()

    fun showErrorToast(@StringRes errorMessage: Int)
}