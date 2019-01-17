package com.blockchain.kyc.dev

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Address
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuStateResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.SupportedDocuments
import com.blockchain.kyc.models.nabu.Tiers
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.NabuSessionTokenResponse
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FakeNabuDataManager : NabuDataManager {

    override fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        offlineTokenResponse: NabuOfflineTokenResponse
    ): Completable {
        Timber.d("Create basic user: $firstName, $lastName, $dateOfBirth")
        return Completable.timer(2, TimeUnit.SECONDS)
    }

    private val address = Address(
        "Line 1",
        "Line 2",
        "City",
        "State",
        "P0STC0D3",
        "DE"
    )

    private val nabuUser = NabuUser(
        firstName = "John",
        lastName = "Doe",
        email = "jdoe@email.com",
        emailVerified = true,
        mobile = "+447123456789",
        mobileVerified = true,
        dob = "2000-01-02",
        address = address,
        state = UserState.Created,
        kycState = KycState.None,
        insertedAt = null,
        updatedAt = null,
        tags = null,
        tiers = Tiers(
            current = 1,
            next = 1,
            selected = 1
        )
    )

    override fun getUser(offlineTokenResponse: NabuOfflineTokenResponse): Single<NabuUser> {
        return Single.just(
            nabuUser
        )
    }

    override fun getCountriesList(scope: Scope): Single<List<NabuCountryResponse>> {
        return Single.just(
            listOf(
                NabuCountryResponse("DE", "Germany", listOf("KYC"), listOf("EEA")),
                NabuCountryResponse("GB", "United Kingdom", listOf("KYC"), listOf("EEA"))
            )
        )
    }

    override fun updateUserWalletInfo(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String
    ): Single<NabuUser> {
        return Single.just(
            nabuUser.copy(
                address = address
            )
        ).delay(500, TimeUnit.MILLISECONDS)
    }

    override fun addAddress(
        offlineTokenResponse: NabuOfflineTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable {
        return Completable.timer(1, TimeUnit.SECONDS)
    }

    override fun recordCountrySelection(
        offlineTokenResponse: NabuOfflineTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOnfidoApiKey(offlineTokenResponse: NabuOfflineTokenResponse): Single<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVeriffToken(offlineTokenResponse: NabuOfflineTokenResponse): Single<VeriffApplicantAndToken> {
        return Single.just(VeriffApplicantAndToken(applicantId = "applicant", token = "token"))
    }

    override fun submitOnfidoVerification(
        offlineTokenResponse: NabuOfflineTokenResponse,
        applicantId: String
    ): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun submitVeriffVerification(
        offlineTokenResponse: NabuOfflineTokenResponse,
        applicantId: String
    ): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStatesList(countryCode: String, scope: Scope): Single<List<NabuStateResponse>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportedDocuments(
        offlineTokenResponse: NabuOfflineTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>> {
        return Single.just(
            listOf(
                SupportedDocuments.DRIVING_LICENCE,
                SupportedDocuments.NATIONAL_IDENTITY_CARD,
                SupportedDocuments.PASSPORT
            )
        )
    }

    override fun registerCampaign(
        offlineTokenResponse: NabuOfflineTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCampaignList(offlineTokenResponse: NabuOfflineTokenResponse): Single<List<String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestJwt(): Single<String> {
        return Single.just("JWT1234")
    }

    override fun getAuthToken(jwt: String): Single<NabuOfflineTokenResponse> {
        return Single.just(NabuOfflineTokenResponse("User123", "Token456"))
    }

    override fun <T> authenticate(
        offlineToken: NabuOfflineTokenResponse,
        singleFunction: (NabuSessionTokenResponse) -> Single<T>
    ): Single<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearAccessToken() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun invalidateToken() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun currentToken(offlineToken: NabuOfflineTokenResponse): Single<NabuSessionTokenResponse> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}