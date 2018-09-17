package com.blockchain.logging

import com.blockchain.testutils.after
import com.blockchain.testutils.before
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

class TimberLoggerTest {

    private val timberMock = mock<Timber.Tree>()

    @get:Rule
    val timber = before {
        Timber.plant(timberMock)
    } after {
        Timber.uproot(timberMock)
    }

    @Test
    fun d() {
        TimberLogger().d("This is a debug message")
        verify(timberMock).d("This is a debug message")
        verifyNoMoreInteractions(timberMock)
    }

    @Test
    fun v() {
        TimberLogger().v("This is a verbose message")
        verify(timberMock).v("This is a verbose message")
        verifyNoMoreInteractions(timberMock)
    }

    @Test
    fun e() {
        TimberLogger().e("This is an error message")
        verify(timberMock).e("This is an error message")
        verifyNoMoreInteractions(timberMock)
    }
}
