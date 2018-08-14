package com.blockchain.kycui.profile

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.mapFromMetadata
import com.blockchain.kyc.util.toSimpleDateString
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`should throw`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiSerialisedString
import java.util.Calendar

class KycProfilePresenterTest {

    private lateinit var subject: KycProfilePresenter
    private val view: KycProfileView = mock()
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
        subject = KycProfilePresenter(
            nabuDataManager,
            metadataManager
        )
        subject.initView(view)
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
    fun `on continue clicked all data correct, metadata fetch failure`() {
        // Arrange
        whenever(view.firstName).thenReturn("Adam")
        whenever(view.lastName).thenReturn("Bennett")
        whenever(view.dateOfBirth).thenReturn(Calendar.getInstance())
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
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = Calendar.getInstance()
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiSerialisedString())))
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toSimpleDateString(),
                offlineToken.mapFromMetadata()
            )
        ).thenReturn(Completable.complete())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp(any())
    }

    @Test
    fun `on continue clicked all data correct, no metadata found`() {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = Calendar.getInstance()
        val offlineToken = NabuCredentialsMetadata("", "")
        val userId = "USER_ID"
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.absent()))
        whenever(nabuDataManager.createUserId()).thenReturn(Single.just(userId))
        whenever(nabuDataManager.getAuthToken(userId))
            .thenReturn(Single.just(offlineToken.mapFromMetadata()))
        whenever(metadataManager.saveToMetadata(offlineToken))
            .thenReturn(Completable.complete())
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toSimpleDateString(),
                offlineToken.mapFromMetadata()
            )
        ).thenReturn(Completable.complete())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp(any())
    }
}