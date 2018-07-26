package com.blockchain.kyc.api

import io.reactivex.Single
import com.blockchain.kyc.models.ApplicantRequest
import com.blockchain.kyc.models.ApplicantResponse
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
}