package com.blockchain.kycui.address

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.mapFromMetadata
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiSerialisedString

class KycHomeAddressPresenterTest {

    private lateinit var subject: KycHomeAddressPresenter
    private val view: KycHomeAddressView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val metadataManager: MetadataManager = mock()

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
            nabuDataManager
        )
        subject.initView(view)
    }

    @Test
    fun `firstLine set but other values not, should disable button`() {
        subject.firstLineSet = true

        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `firstLineSet and city set but zipCode not, should disable button`() {
        subject.firstLineSet = true
        subject.citySet = true

        verify(view, times(2)).setButtonEnabled(false)
    }

    @Test
    fun `all values set, should enable button`() {
        subject.firstLineSet = true
        subject.citySet = true
        subject.zipCodeSet = true

        verify(view, times(2)).setButtonEnabled(false)
        verify(view).setButtonEnabled(true)
    }

    @Test
    fun `on continue clicked firstLine empty should throw IllegalStateException`() {
        whenever(view.firstLine).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked city empty should throw IllegalStateException`() {
        whenever(view.firstLine).thenReturn("1")
        whenever(view.city).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked date of state empty should throw IllegalStateException`() {
        whenever(view.firstLine).thenReturn("1")
        whenever(view.city).thenReturn("2")
        whenever(view.zipCode).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked all data correct, metadata fetch failure`() {
        // Arrange
        whenever(view.firstLine).thenReturn("1")
        whenever(view.city).thenReturn("2")
        whenever(view.zipCode).thenReturn("3")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }

    @Test
    fun `on continue clicked all data correct, metadata fetch success`() {
        // Arrange
        val firstLine = "1"
        val city = "2"
        val zipCode = "3"
        val countryCode = "UK"
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.firstLine).thenReturn(firstLine)
        whenever(view.city).thenReturn(city)
        whenever(view.zipCode).thenReturn(zipCode)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiSerialisedString())))
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
        verify(view).continueSignUp()
    }

    @Test
    fun `countryCodeSingle should return sorted country map`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiSerialisedString())))
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
}