package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.NABU_COUNTRIES
import com.blockchain.kyc.api.nabu.Nabu
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import io.reactivex.Single
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NabuService @Inject constructor(
    val environmentConfig: EnvironmentConfig,
    @Named("kotlin") retrofit: Retrofit
) {

    private val service: Nabu = retrofit.create(Nabu::class.java)
    private val apiPath = environmentConfig.apiUrl

    internal fun getEeaCountries(
        path: String = apiPath + NABU_COUNTRIES,
        userAgent: String
    ): Single<List<NabuCountryResponse>> = service.getHomebrewRegions(
        path,
        "eea",
        userAgent
    )
}