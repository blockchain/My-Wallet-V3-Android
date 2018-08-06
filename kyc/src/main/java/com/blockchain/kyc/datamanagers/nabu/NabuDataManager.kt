package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.services.nabu.NabuService
import io.reactivex.Single
import javax.inject.Inject

class NabuDataManager @Inject constructor(
    private val nabuService: NabuService
) {

    // TODO: Add Class for easy User Agent formatting AND-1320
    internal fun isInEeaCountry(countryCode: String, userAgent: String): Single<Boolean> =
        nabuService.getEeaCountries(userAgent = userAgent)
            .map { it.containsCountry(countryCode) }

    private fun List<NabuCountryResponse>.containsCountry(countryCode: String): Boolean =
        this.any { it.code.equals(countryCode, ignoreCase = true) }
}