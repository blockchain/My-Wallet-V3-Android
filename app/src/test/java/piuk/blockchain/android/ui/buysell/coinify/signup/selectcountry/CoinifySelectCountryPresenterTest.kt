package piuk.blockchain.androidbuysellui.ui.signup

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry.CoinifySelectCountryPresenter
import piuk.blockchain.android.ui.buysell.coinify.signup.selectcountry.CoinifySelectCountryView
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class CoinifySelectCountryPresenterTest : RxTest() {

    private lateinit var subject: CoinifySelectCountryPresenter

    private val view: CoinifySelectCountryView = mock()
    private val buyDataManager: BuyDataManager = mock()

    @Before
    fun setup() {
        subject =
            CoinifySelectCountryPresenter(
                buyDataManager
            )
        subject.initView(view)
    }

    @Test
    fun `onViewReady autoselect success`() {

        // Arrange
        whenever(buyDataManager.countryCode).thenReturn(Observable.just("ZA"))

        // Act
        subject.onViewReady()

        // Assert
        verify(view).onSetCountryPickerData(any())
        verify(view).onAutoSelectCountry(203)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady autoselect fail`() {

        // Arrange
        whenever(buyDataManager.countryCode).thenReturn(Observable.just("NONE"))

        // Act
        subject.onViewReady()

        // Assert
        verify(view).onSetCountryPickerData(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `collectDataAndContinue coinify not allowed`() {

        // Arrange
        whenever(buyDataManager.countryCode).thenReturn(Observable.just("AF"))
        whenever(buyDataManager.isInCoinifyCountry(any())).thenReturn(Observable.just(false))
        subject.onViewReady()

        // Act
        subject.collectDataAndContinue(0)

        // Assert
        verify(view).onSetCountryPickerData(any())
        verify(view).onStartInvalidCountry()
        verify(view).onAutoSelectCountry(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `collectDataAndContinue coinify is allowed`() {

        // Arrange
        whenever(buyDataManager.countryCode).thenReturn(Observable.just("AF"))
        whenever(buyDataManager.isInCoinifyCountry(any())).thenReturn(Observable.just(true))
        subject.onViewReady()

        // Act
        subject.collectDataAndContinue(0)

        // Assert
        verify(view).onSetCountryPickerData(any())
        verify(view).onStartVerifyEmail("AF")
        verify(view).onAutoSelectCountry(any())
        verifyNoMoreInteractions(view)
    }
}