package com.blockchain.kyc.datamanagers.onfido

import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.kyc.services.onfido.OnfidoService
import io.reactivex.Single

class OnfidoDataManager(private val onfidoService: OnfidoService) {

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