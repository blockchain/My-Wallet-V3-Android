package com.blockchain.kyc.datamanagers

import io.reactivex.Single
import com.blockchain.kyc.models.ApplicantResponse
import com.blockchain.kyc.services.OnfidoService
import javax.inject.Inject

class OnfidoDataManager @Inject constructor(
    private val onfidoService: OnfidoService
) {

    /**
     * Creates a new KYC application in Onfido, and returns an [ApplicantResponse] object.
     *
     * @param firstName The applicant's first name
     * @param lastName The applicant's surname
     * @param apiToken Our mobile Onfido API token
     *
     * @return An [ApplicantResponse] wrapped in a [Single]
     */
    fun createApplicant(
        firstName: String,
        lastName: String,
        apiToken: String
    ): Single<ApplicantResponse> =
        onfidoService.createApplicant(
            firstName = firstName,
            lastName = lastName,
            apiToken = apiToken
        )
}