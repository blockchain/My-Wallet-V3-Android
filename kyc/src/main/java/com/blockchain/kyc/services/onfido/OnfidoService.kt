package com.blockchain.kyc.services.onfido

import com.blockchain.kyc.api.onfido.APPLICANTS
import com.blockchain.kyc.api.onfido.ONFIDO_LIVE_BASE
import com.blockchain.kyc.api.onfido.Onfido
import com.blockchain.kyc.models.onfido.ApplicantRequest
import com.blockchain.kyc.models.onfido.ApplicantResponse
import io.reactivex.Single
import retrofit2.Retrofit

class OnfidoService(retrofit: Retrofit) {

    private val service: Onfido = retrofit.create(Onfido::class.java)

    internal fun createApplicant(
        path: String = "$ONFIDO_LIVE_BASE$APPLICANTS",
        firstName: String,
        lastName: String,
        apiToken: String
    ): Single<ApplicantResponse> =
        service.createApplicant(
            path,
            ApplicantRequest(firstName, lastName),
            getFormattedToken(apiToken)
        )

    private fun getFormattedToken(apiToken: String) = "Token token=$apiToken"
}