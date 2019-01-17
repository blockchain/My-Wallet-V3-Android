package com.blockchain.kycui.address

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Address
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kycui.address.models.AddressModel
import com.blockchain.nabu.NabuToken
import com.blockchain.validOfflineToken
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.PhoneVerificationQuery

class KycHomeAddressPresenterTest {

    private lateinit var subject: KycHomeAddressPresenter
    private val view: KycHomeAddressView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val nabuToken: NabuToken = mock()
    private val phoneVerificationQuery: PhoneVerificationQuery = mock()
    private val tier2Decision: Tier2Decision = mock {
        on { progressToTier2() } `it returns` Single.just(Tier2Decision.NextStep.Tier2Continue)
    }

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycHomeAddressPresenter(
            nabuToken,
            nabuDataManager,
            tier2Decision,
            phoneVerificationQuery
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady first line emitted empty should disable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(addressModel()))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `onViewReady city emitted empty should disable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine = "FIRST_LINE")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `onViewReady country emitted empty should disable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine = "FIRST_LINE", city = "CITY")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `onViewReady country emitted complete should enable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(
                Observable.just(
                    addressModel(firstLine = "FIRST_LINE", city = "CITY", postCode = "POST_CODE")
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(true)
    }

    @Test
    fun `onViewReady no data to restore`() {
        // Arrange
        whenever(view.address).thenReturn(Observable.just(addressModel()))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser()))
        // Act
        subject.onViewReady()
        // Assert
        verify(view, never()).restoreUiState(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `onViewReady data already input, should not attempt to restore`() {
        // Arrange
        whenever(view.address).thenReturn(Observable.just(addressModel(firstLine = "FIRST_LINE")))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        // Act
        subject.onViewReady()
        // Assert
        verify(view, never()).restoreUiState(any(), any(), any(), any(), any(), any())
        verify(nabuDataManager, never()).getUser(validOfflineToken)
    }

    @Test
    fun `onViewReady has address to restore`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(addressModel()))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val firstLine = "FIRST_LINE"
        val city = "CITY"
        val postCode = "POST_CODE"
        val country = "COUNTRY_CODE"
        val countryName = "COUNTRY_NAME"
        val address = Address(
            line1 = firstLine,
            line2 = null,
            city = city,
            state = null,
            postCode = postCode,
            countryCode = country
        )
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser().copy(address = address)))
        val countryList =
            listOf(NabuCountryResponse(country, countryName, emptyList(), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).restoreUiState(firstLine, null, city, null, postCode, countryName)
    }

    @Test
    fun `onViewReady has user but no address`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(addressModel()))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser().copy(address = null)))
        // Act
        subject.onViewReady()
        // Assert
        verify(view, never()).restoreUiState(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `onViewReady data restoration fails silently`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(addressModel()))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view, never()).restoreUiState(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `on continue clicked all data correct, metadata fetch failure`() {
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine, city, zipCode, countryCode)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
        whenever(
            nabuDataManager.addAddress(
                validOfflineToken,
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }

    @Test
    fun `on continue clicked all data correct, phone number unverified`() {
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine, city, zipCode, countryCode)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.addAddress(
                validOfflineToken,
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        givenPhoneNumberNotVerified()
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueToMobileVerification(countryCode)
    }

    @Test
    fun `on continue clicked all data correct, phone number verified`() {
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine, city, zipCode, countryCode)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.addAddress(
                validOfflineToken,
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        givenPhoneNumberVerified()
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(nabuDataManager.updateUserWalletInfo(validOfflineToken, jwt))
            .thenReturn(Single.just(getBlankNabuUser()))
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueToOnfidoSplash(countryCode)
    }

    @Test
    fun `on continue clicked and tier2 decision reports to not continue, tier1 is complete`() {
        whenever(tier2Decision.progressToTier2()).itReturns(Single.just(Tier2Decision.NextStep.Tier1Complete))
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine, city, zipCode, countryCode)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.addAddress(
                validOfflineToken,
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        givenPhoneNumberNotVerified()
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).tier1Complete()
    }

    @Test
    fun `on continue clicked and tier2 decision reports to get more info, tier2 continues`() {
        whenever(
            tier2Decision.progressToTier2()
        ).itReturns(Single.just(Tier2Decision.NextStep.Tier2ContinueTier1NeedsMoreInfo))
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        whenever(view.address)
            .thenReturn(Observable.just(addressModel(firstLine, city, zipCode, countryCode)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.addAddress(
                validOfflineToken,
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        givenPhoneNumberNotVerified()
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueToTier2MoreInfoNeeded(countryCode)
    }

    private fun givenPhoneNumberVerified() {
        whenever(phoneVerificationQuery.isPhoneNumberVerified()).thenReturn(Single.just(true))
    }

    private fun givenPhoneNumberNotVerified() {
        whenever(phoneVerificationQuery.isPhoneNumberVerified()).thenReturn(Single.just(false))
    }

    @Test
    fun `countryCodeSingle should return sorted country map`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryList = listOf(
            NabuCountryResponse("DE", "Germany", emptyList(), emptyList()),
            NabuCountryResponse("UK", "United Kingdom", emptyList(), emptyList()),
            NabuCountryResponse("FR", "France", emptyList(), emptyList())
        )
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        // Act
        val testObserver = subject.countryCodeSingle.test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val sortedMap = testObserver.values().first()
        sortedMap.size `should equal to` 3
        val expectedMap = sortedMapOf(
            "France" to "FR",
            "Germany" to "DE",
            "United Kingdom" to "UK"
        )
        sortedMap `should equal` expectedMap
    }

    private fun addressModel(
        firstLine: String = "",
        city: String = "",
        postCode: String = "",
        country: String = ""
    ): AddressModel = AddressModel(
        firstLine,
        null,
        city,
        null,
        postCode,
        country
    )
}