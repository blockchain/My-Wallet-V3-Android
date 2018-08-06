package com.blockchain.kyc.api.onfido

import com.blockchain.kyc.models.onfido.ApplicantRequest
import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.kyc.models.onfido.OnfidoCheckOptions
import com.blockchain.kyc.models.onfido.OnfidoCheckResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface Onfido {

    @POST
    fun createApplicant(
        @Url url: String,
        @Body applicantRequest: ApplicantRequest,
        @Header("Authorization") apiToken: String
    ): Single<ApplicantResponse>

    @POST
    fun createCheck(
        @Url url: String,
        @Body body: OnfidoCheckOptions,
        @Header("Authorization") apiToken: String
    ): Single<OnfidoCheckResponse>
}