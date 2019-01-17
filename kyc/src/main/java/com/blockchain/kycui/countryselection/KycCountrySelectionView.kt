package com.blockchain.kycui.countryselection

import com.blockchain.kycui.countryselection.models.CountrySelectionState
import com.blockchain.kycui.countryselection.util.CountryDisplayModel
import piuk.blockchain.androidcoreui.ui.base.View

internal interface KycCountrySelectionView : View {

    val regionType: RegionType

    fun continueFlow(countryCode: String)

    fun invalidCountry(displayModel: CountryDisplayModel)

    fun renderUiState(state: CountrySelectionState)

    fun requiresStateSelection()
}