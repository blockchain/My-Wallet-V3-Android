package com.blockchain.kyc.services.onfido

import com.blockchain.kyc.api.onfido.APPLICANTS
import com.blockchain.kyc.api.onfido.CHECKS
import com.blockchain.kyc.api.onfido.ONFIDO_LIVE_BASE
import com.blockchain.kyc.api.onfido.Onfido
import com.blockchain.kyc.models.onfido.ApplicantRequest
import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.kyc.models.onfido.OnfidoCheckOptions
import com.blockchain.kyc.models.onfido.OnfidoCheckResponse
import io.reactivex.Single
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

    internal fun createCheck(
        path: String = "$ONFIDO_LIVE_BASE$APPLICANTS",
        applicantId: String,
        apiToken: String
    ): Single<OnfidoCheckResponse> =
        service.createCheck(
            "$path$applicantId/$CHECKS",
            OnfidoCheckOptions.getDefault(),
            getFormattedToken(apiToken)
        )

    private fun getFormattedToken(apiToken: String) = "Token token=$apiToken"
}
