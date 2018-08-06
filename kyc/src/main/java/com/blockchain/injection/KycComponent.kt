package com.blockchain.injection

import com.blockchain.kycui.countryselection.KycCountrySelectionFragment
import dagger.Subcomponent

@Subcomponent
interface KycComponent {

    fun inject(kycCountrySelectionFragment: KycCountrySelectionFragment)
}