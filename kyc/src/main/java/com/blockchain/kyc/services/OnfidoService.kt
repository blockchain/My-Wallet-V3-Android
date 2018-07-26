package com.blockchain.kyc.services

import io.reactivex.Single
import com.blockchain.kyc.api.APPLICANTS
import com.blockchain.kyc.api.ONFIDO_LIVE_BASE
import com.blockchain.kyc.api.Onfido
import com.blockchain.kyc.models.ApplicantRequest
import com.blockchain.kyc.models.ApplicantResponse
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OnfidoService @Inject constructor(@Named("kotlin") retrofit: Retrofit) {

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