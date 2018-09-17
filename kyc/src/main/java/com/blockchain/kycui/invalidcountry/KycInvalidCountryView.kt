package com.blockchain.kycui.invalidcountry

import piuk.blockchain.androidcoreui.ui.base.View

interface KycInvalidCountryView : View {

    val countryCode: String

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun finishPage()
}