package com.blockchain.injection

import com.blockchain.kycui.countryselection.KycCountrySelectionFragment
import com.blockchain.kycui.profile.KycProfileFragment
import dagger.Subcomponent

@Subcomponent
interface KycComponent {

    fun inject(kycCountrySelectionFragment: KycCountrySelectionFragment)

    fun inject(kycCountrySelectionFragment: KycProfileFragment)
}