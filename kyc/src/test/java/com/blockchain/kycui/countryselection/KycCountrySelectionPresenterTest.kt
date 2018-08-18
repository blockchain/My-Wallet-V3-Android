package com.blockchain.kycui.countryselection

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kycui.countryselection.models.CountrySelectionState
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class KycCountrySelectionPresenterTest {

    private lateinit var subject: KycCountrySelectionPresenter
    private val view: KycCountrySelectionView = mock()
    private val nabuDataManager: NabuDataManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycCountrySelectionPresenter(nabuDataManager)
        subject.initView(view)
    }

    @Test
    fun `onViewReady error loading countries`() {
        // Arrange
        whenever(nabuDataManager.getCountriesList(Scope.None)).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).renderUiState(any(CountrySelectionState.Loading::class))
        verify(view).renderUiState(any(CountrySelectionState.Error::class))
    }

    @Test
    fun `onViewReady loading countries success`() {
        // Arrange
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).renderUiState(any(CountrySelectionState.Loading::class))
        verify(view).renderUiState(any(CountrySelectionState.Data::class))
    }

    @Test
    fun `onCountrySelected not found`() {
        // Arrange
        val countryCode = "US"
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(emptyList()))
        // Act
        subject.onCountrySelected(countryCode)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).invalidCountry(countryCode)
    }

    @Test
    fun `onCountrySelected country found`() {
        // Arrange
        val countryCode = "UK"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        // Act
        subject.onCountrySelected(countryCode)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).continueFlow(countryCode)
    }
}