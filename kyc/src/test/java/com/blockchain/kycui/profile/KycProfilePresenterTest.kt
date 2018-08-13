package com.blockchain.kycui.profile

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kycui.profile.models.ProfileModel
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.`should throw`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import java.util.Calendar

class KycProfilePresenterTest {

    private lateinit var subject: KycProfilePresenter
    private val view: KycProfileView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val metadataManager: MetadataManager = mock()

    @Before
    fun setUp() {
        subject = KycProfilePresenter(
            nabuDataManager,
            settingsDataManager,
            payloadDataManager,
            metadataManager
        ).apply { initView(view) }
    }

    @Test
    fun `firstName set but other values not, should disable button`() {
        subject.firstNameSet = true

        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `firstName and lastName set but DoB not, should disable button`() {
        subject.firstNameSet = true
        subject.lastNameSet = true

        verify(view, times(2)).setButtonEnabled(false)
    }

    @Test
    fun `all values set, should enable button`() {
        subject.firstNameSet = true
        subject.lastNameSet = true
        subject.dateSet = true

        verify(view, times(2)).setButtonEnabled(false)
        verify(view).setButtonEnabled(true)
    }

    @Test
    fun `on continue clicked firstName empty should throw IllegalStateException`() {
        whenever(view.firstName).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked lastName empty should throw IllegalStateException`() {
        whenever(view.firstName).thenReturn("Adam")
        whenever(view.lastName).thenReturn("");

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked date of birth null should throw IllegalStateException`() {
        whenever(view.firstName).thenReturn("Adam")
        whenever(view.lastName).thenReturn("Bennett")
        whenever(view.dateOfBirth).thenReturn(null);

        {
            subject.onContinueClicked()
        } `should throw` IllegalStateException::class
    }

    @Test
    fun `on continue clicked all data correct`() {
        // Arrange
        whenever(view.firstName).thenReturn("Adam")
        whenever(view.lastName).thenReturn("Bennett")
        whenever(view.dateOfBirth).thenReturn(Calendar.getInstance())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).continueSignUp(any(ProfileModel::class))
    }
}