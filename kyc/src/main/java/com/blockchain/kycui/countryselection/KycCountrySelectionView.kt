package com.blockchain.kycui.countryselection

import com.blockchain.kycui.countryselection.models.CountrySelectionState
import piuk.blockchain.androidcoreui.ui.base.View

interface KycCountrySelectionView : View {

    fun continueFlow()

    fun invalidCountry(countryCode: String)

    fun renderUiState(state: CountrySelectionState)
}