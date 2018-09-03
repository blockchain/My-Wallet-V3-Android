package com.blockchain.kycui.address

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.address.models.AddressModel
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class KycHomeAddressPresenterTest {

    private lateinit var subject: KycHomeAddressPresenter
    private val view: KycHomeAddressView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val settingsDataManager: SettingsDataManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycHomeAddressPresenter(
            metadataManager,
            nabuDataManager,
            settingsDataManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady first line emitted empty should disable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(AddressModel("", null, "", null, "", "")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `onViewReady city emitted empty should disable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(AddressModel("FIRST_LINE", null, "", null, "", "")))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `onViewReady country emitted empty should disable button`() {
        // Arrange
        whenever(view.address)
            .thenReturn(Observable.just(AddressModel("FIRST_LINE", null, "CITY", null, "", "")))
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
                    AddressModel(
                        "FIRST_LINE",
                        null,
                        "CITY",
                        null,
                        "POST_CODE",
                        ""
                    )
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(true)
    }

    @Test
    fun `on continue clicked all data correct, metadata fetch failure`() {
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.address)
            .thenReturn(
                Observable.just(
                    AddressModel(
                        firstLine,
                        null,
                        city,
                        null,
                        zipCode,
                        countryCode
                    )
                )
            )
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        whenever(
            nabuDataManager.addAddress(
                offlineToken.mapFromMetadata(),
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
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.address)
            .thenReturn(
                Observable.just(
                    AddressModel(
                        firstLine,
                        null,
                        city,
                        null,
                        zipCode,
                        countryCode
                    )
                )
            )
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(
            nabuDataManager.addAddress(
                offlineToken.mapFromMetadata(),
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(Settings()))
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
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.address)
            .thenReturn(
                Observable.just(
                    AddressModel(
                        firstLine,
                        null,
                        city,
                        null,
                        zipCode,
                        countryCode
                    )
                )
            )
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(
            nabuDataManager.addAddress(
                offlineToken.mapFromMetadata(),
                firstLine,
                null,
                city,
                null,
                zipCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        val settings: Settings = mock()
        whenever(settings.isSmsVerified).thenReturn(true)
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(settings))
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(nabuDataManager.updateUserWalletInfo(offlineToken.mapFromMetadata(), jwt))
            .thenReturn(Single.just(getNabuUser()))
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueToOnfidoSplash()
    }

    @Test
    fun `countryCodeSingle should return sorted country map`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
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

    private fun getNabuUser(): NabuUser = NabuUser(
        "",
        "",
        "",
        "",
        false,
        null,
        UserState.None,
        KycState.None,
        "",
        ""
    )
}