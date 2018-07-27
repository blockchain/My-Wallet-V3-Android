package com.blockchain.kyc.datamanagers

import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.kyc.models.onfido.OnfidoCheckResponse
import com.blockchain.kyc.services.onfido.OnfidoService
import io.reactivex.Single
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

    /**
     * Creates an express KYC check with Onfido once documents are submitted. Returns an [OnfidoCheckResponse] object.
     *
     * @param applicantId The ID of the applicant you wish to create a check for
     * @param apiToken Our mobile Onfido API token
     *
     * * @return An [OnfidoCheckResponse] wrapped in a [Single]
     */
    fun createCheck(
        applicantId: String,
        apiToken: String
    ): Single<OnfidoCheckResponse> =
        onfidoService.createCheck(
            applicantId = applicantId,
            apiToken = apiToken
        )
}