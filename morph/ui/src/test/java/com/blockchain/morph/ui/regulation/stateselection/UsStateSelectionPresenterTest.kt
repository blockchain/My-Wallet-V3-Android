package com.blockchain.morph.ui.regulation.stateselection

import android.app.Activity
import com.blockchain.morph.regulation.UsState
import com.blockchain.morph.regulation.UsStatesDataManager
import com.blockchain.morph.ui.R
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class UsStateSelectionPresenterTest {

    private lateinit var subject: UsStateSelectionPresenter
    private val usStatesDataManager: UsStatesDataManager = mock()
    private val view: UsStateSelectionView = mock()

    @Before
    fun setUp() {
        subject = UsStateSelectionPresenter(usStatesDataManager)
        subject.initView(view)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateAmericanState state not found`() {
        // Arrange
        val invalidState = "invalid"
        // Act
        subject.updateAmericanState(invalidState)
        // Assert
        verifyZeroInteractions(usStatesDataManager)
    }

    @Test
    fun `updateAmericanState with valid state, whitelist check fails`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(usStatesDataManager.isStateWhitelisted(UsState(state, stateCode)))
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(usStatesDataManager).isStateWhitelisted(UsState(state, stateCode))
        verifyNoMoreInteractions(usStatesDataManager)
        verify(view).finishActivityWithResult(Activity.RESULT_CANCELED)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `updateAmericanState with valid state, state is not whitelisted`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(usStatesDataManager.isStateWhitelisted(UsState(state, stateCode)))
            .thenReturn(Observable.just(false))
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(usStatesDataManager).isStateWhitelisted(UsState(state, stateCode))
        verifyNoMoreInteractions(usStatesDataManager)
        verify(view).onError(R.string.morph_unavailable_in_state)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `updateAmericanState with valid state, state is whitelisted but storing state fails`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(usStatesDataManager.isStateWhitelisted(UsState(state, stateCode)))
            .thenReturn(Observable.just(true))
        whenever(usStatesDataManager.setState(any()))
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(usStatesDataManager).isStateWhitelisted(UsState(state, stateCode))
        verify(usStatesDataManager).setState(any())
        verifyNoMoreInteractions(usStatesDataManager)
        verify(view).finishActivityWithResult(Activity.RESULT_CANCELED)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `updateAmericanState with valid state, state is whitelisted`() {
        // Arrange
        val state = "California"
        val stateCode = "CA"
        whenever(usStatesDataManager.isStateWhitelisted(UsState(state, stateCode)))
            .thenReturn(Observable.just(true))
        whenever(usStatesDataManager.setState(any()))
            .thenReturn(Completable.complete())
        // Act
        subject.updateAmericanState(state)
        // Assert
        verify(usStatesDataManager).isStateWhitelisted(UsState(state, stateCode))
        verify(usStatesDataManager).setState(any())
        verifyNoMoreInteractions(usStatesDataManager)
        verify(view).finishActivityWithResult(Activity.RESULT_OK)
        verifyNoMoreInteractions(view)
    }
}
