package com.blockchain.kycui.countryselection

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
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
    fun `onCountrySelected valid country`() {
        // Arrange
        val countryCode = "UK"
        whenever(nabuDataManager.isInEeaCountry(countryCode))
            .thenReturn(Single.just(true))
        // Act
        subject.onCountrySelected(countryCode)
        // Assert
        verify(nabuDataManager).isInEeaCountry(countryCode)
        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).continueFlow()
    }

    @Test
    fun `onCountrySelected invalid country`() {
        // Arrange
        val countryCode = "US"
        whenever(nabuDataManager.isInEeaCountry(countryCode))
            .thenReturn(Single.just(false))
        // Act
        subject.onCountrySelected(countryCode)
        // Assert
        verify(nabuDataManager).isInEeaCountry(countryCode)
        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).invalidCountry()
    }

    @Test
    fun `onCountrySelected error`() {
        // Arrange
        val countryCode = "US"
        whenever(nabuDataManager.isInEeaCountry(countryCode))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onCountrySelected(countryCode)
        // Assert
        verify(nabuDataManager).isInEeaCountry(countryCode)
        verify(view).showProgress()
        verify(view).hideProgress()
        verify(view).showErrorToast(any())
    }
}