package com.blockchain.kycui.countryselection.models

import android.support.annotation.StringRes
import com.blockchain.kyc.models.nabu.NabuCountryResponse

sealed class CountrySelectionState {
    object Loading : CountrySelectionState()
    data class Error(@StringRes val errorMessage: Int) : CountrySelectionState()
    data class Data(val countriesList: List<NabuCountryResponse>) : CountrySelectionState()
}