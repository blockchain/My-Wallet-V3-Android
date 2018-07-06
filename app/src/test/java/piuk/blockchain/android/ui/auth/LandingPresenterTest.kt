package piuk.blockchain.android.ui.auth

import android.content.Context
import android.support.v7.app.AlertDialog
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.Environment
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.data.datamanagers.PromptManager
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class LandingPresenterTest {

    private lateinit var subject: LandingPresenter
    private val mockActivity: LandingView = mock()
    private val mockContext: Context = mock()
    private var environmentSettings: EnvironmentConfig =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private var promptManager: PromptManager =
        mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)

    @Before
    fun setUp() {
        subject = LandingPresenter(
            environmentSettings,
            promptManager
        )
        subject.initView(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady show debug`() {
        // Arrange
        whenever(environmentSettings.shouldShowDebugMenu()).thenReturn(true)
        val environment = Environment.fromString("env_prod")
        whenever(environmentSettings.environment).thenReturn(environment)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).showToast("Current environment: env_prod", ToastCustom.TYPE_GENERAL)
        verify(mockActivity).showDebugMenu()
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady no debug`() {
        // Arrange
        whenever(environmentSettings.shouldShowDebugMenu()).thenReturn(false)
        // Act
        subject.onViewReady()
        // Assert
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun initPreLoginPrompts() {
        // Arrange
        whenever(promptManager.getPreLoginPrompts(any()))
            .thenReturn(Observable.just(listOf(mock(AlertDialog::class))))
        // Act
        subject.initPreLoginPrompts(mockContext)
        // Assert
        verify(promptManager).getPreLoginPrompts(mockContext)
        verify(mockActivity).showWarningPrompt(any())
    }
}
