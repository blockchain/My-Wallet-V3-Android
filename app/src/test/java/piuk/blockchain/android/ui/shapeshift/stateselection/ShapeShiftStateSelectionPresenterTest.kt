package piuk.blockchain.android.ui.shapeshift.stateselection

import android.app.Activity
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.shapeshift.data.State
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager

class ShapeShiftStateSelectionPresenterTest {

    private lateinit var subject: ShapeShiftStateSelectionPresenter
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val view: ShapeShiftStateSelectionView = mock()

    @Before
    fun setUp() {
        subject = ShapeShiftStateSelectionPresenter(walletOptionsDataManager, shapeShiftDataManager)
        subject.initView(view)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateAmericanState state not found`() {
        // Arrange
        val invalidState = "invalid"
        // Act
        subject.updateAmericanState(invalidState)
        // Assert
        verifyZeroInteractions(walletOptionsDataManager)
        verifyZeroInteractions(shapeShiftDataManager)
    }

    @Test
    fun `updateAmericanState with valid state, whitelist check fails`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(walletOptionsDataManager.isStateWhitelisted(stateCode))
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(walletOptionsDataManager).isStateWhitelisted(stateCode)
        verifyNoMoreInteractions(walletOptionsDataManager)
        verify(view).finishActivityWithResult(Activity.RESULT_CANCELED)
        verifyNoMoreInteractions(view)
        verifyZeroInteractions(shapeShiftDataManager)
    }

    @Test
    fun `updateAmericanState with valid state, state is not whitelisted`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(walletOptionsDataManager.isStateWhitelisted(stateCode))
            .thenReturn(Observable.just(false))
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(walletOptionsDataManager).isStateWhitelisted(stateCode)
        verifyNoMoreInteractions(walletOptionsDataManager)
        verify(view).onError(R.string.morph_unavailable_in_state)
        verifyNoMoreInteractions(view)
        verifyZeroInteractions(shapeShiftDataManager)
    }

    @Test
    fun `updateAmericanState with valid state, state is whitelisted but storing state fails`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(walletOptionsDataManager.isStateWhitelisted(stateCode))
            .thenReturn(Observable.just(true))
        whenever(shapeShiftDataManager.setState(any(State::class)))
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(walletOptionsDataManager).isStateWhitelisted(stateCode)
        verifyNoMoreInteractions(walletOptionsDataManager)
        verify(shapeShiftDataManager).setState(any(State::class))
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).finishActivityWithResult(Activity.RESULT_CANCELED)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `updateAmericanState with valid state, state is whitelisted`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(walletOptionsDataManager.isStateWhitelisted(stateCode))
            .thenReturn(Observable.just(true))
        whenever(shapeShiftDataManager.setState(any(State::class)))
            .thenReturn(Completable.complete())
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(walletOptionsDataManager).isStateWhitelisted(stateCode)
        verifyNoMoreInteractions(walletOptionsDataManager)
        verify(shapeShiftDataManager).setState(any(State::class))
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).finishActivityWithResult(Activity.RESULT_OK)
        verifyNoMoreInteractions(view)
    }
}