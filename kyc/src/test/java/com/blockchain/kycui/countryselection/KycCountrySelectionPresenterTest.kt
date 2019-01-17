package com.blockchain.kycui.countryselection

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuStateResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kycui.countryselection.models.CountrySelectionState
import com.blockchain.kycui.countryselection.util.CountryDisplayModel
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
        whenever(view.regionType).thenReturn(RegionType.Country)
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
        whenever(view.regionType).thenReturn(RegionType.Country)
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
    fun `onViewReady loading states success`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        whenever(nabuDataManager.getStatesList("US", Scope.None))
            .thenReturn(Single.just(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(nabuDataManager).getStatesList("US", Scope.None)
        verify(view).renderUiState(any(CountrySelectionState.Loading::class))
        verify(view).renderUiState(any(CountrySelectionState.Data::class))
    }

    @Test
    fun `onRegionSelected requires state selection`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(emptyList()))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).requiresStateSelection()
    }

    @Test
    fun `onRegionSelected state not found, not in kyc region`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        whenever(nabuDataManager.getStatesList("US", Scope.None))
            .thenReturn(Single.just(emptyList()))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US",
            isState = true,
            state = "US-AL"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getStatesList("US", Scope.None)
        verify(view).invalidCountry(countryDisplayModel)
    }

    @Test
    fun `onRegionSelected state found, in kyc region`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.State)
        val countryCode = "US"
        whenever(nabuDataManager.getStatesList("US", Scope.None))
            .thenReturn(
                Single.just(
                    listOf(
                        NabuStateResponse(
                            code = "US-AL",
                            name = "Alabama",
                            scopes = listOf("KYC"),
                            countryCode = "US"
                        )
                    )
                )
            )
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US",
            isState = true,
            state = "US-AL"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getStatesList("US", Scope.None)
        verify(view).continueFlow(countryCode)
    }

    @Test
    fun `onRegionSelected country found`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryCode = "UK"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", listOf("KYC"), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        val countryDisplayModel = CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "UK"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).continueFlow(countryCode)
    }

    @Test
    fun `onRegionSelected country found but is US so requires state selection`() {
        // Arrange
        whenever(view.regionType).thenReturn(RegionType.Country)
        val countryList =
            listOf(NabuCountryResponse("US", "United States", listOf("KYC"), emptyList()))
        whenever(nabuDataManager.getCountriesList(Scope.None))
            .thenReturn(Single.just(countryList))
        val countryDisplayModel = CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        )
        // Act
        subject.onRegionSelected(countryDisplayModel)
        // Assert
        verify(nabuDataManager).getCountriesList(Scope.None)
        verify(view).requiresStateSelection()
    }
}
