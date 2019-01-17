package com.blockchain.kycui.profile

import com.blockchain.android.testutils.rxInit
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kyc.util.toISO8601DateString
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.testutils.date
import com.blockchain.validOfflineToken
import com.blockchain.validOfflineTokenMetadata
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.`should throw`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.util.Locale

class KycProfilePresenterTest {

    private lateinit var subject: KycProfilePresenter
    private val view: KycProfileView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val metadataRepository: MetadataRepository = mock()
    private val nabuToken: NabuToken = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycProfilePresenter(
            nabuToken,
            nabuDataManager,
            metadataRepository
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
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(
            metadataRepository.loadMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                NabuCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.error { Throwable() })
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
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            metadataRepository.loadMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                NabuCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.just(validOfflineTokenMetadata))
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
                validOfflineToken
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
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        val offlineToken = NabuCredentialsMetadata("", "")
        val jwt = "JWT"
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            metadataRepository.loadMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                NabuCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.empty())
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(nabuDataManager.getAuthToken(jwt))
            .thenReturn(Single.just(offlineToken.mapFromMetadata()))
        whenever(
            metadataRepository.saveMetadata(
                offlineToken,
                NabuCredentialsMetadata::class.java,
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Completable.complete())
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
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
    fun `on continue clicked all data correct, metadata invalid`() {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        val invalidToken = NabuCredentialsMetadata("", "")
        val jwt = "JWT"
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            metadataRepository.loadMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                NabuCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.just(invalidToken))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error(MetadataNotFoundException("Nabu Token is empty")))
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(nabuDataManager.getAuthToken(jwt))
            .thenReturn(Single.just(validOfflineToken))
        whenever(
            metadataRepository.saveMetadata(
                validOfflineTokenMetadata,
                NabuCredentialsMetadata::class.java,
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Completable.complete())
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
                validOfflineToken
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
    fun `on continue clicked all data correct, user conflict`() {
        // Arrange
        val firstName = "Adam"
        val lastName = "Bennett"
        val dateOfBirth = date(Locale.US, 2014, 8, 10)
        val countryCode = "UK"
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.firstName).thenReturn(firstName)
        whenever(view.lastName).thenReturn(lastName)
        whenever(view.dateOfBirth).thenReturn(dateOfBirth)
        whenever(view.countryCode).thenReturn(countryCode)
        whenever(
            metadataRepository.loadMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
                NabuCredentialsMetadata::class.java
            )
        ).thenReturn(Maybe.empty())
        val jwt = "JTW"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(nabuDataManager.getAuthToken(jwt))
            .thenReturn(Single.just(offlineToken.mapFromMetadata()))
        whenever(
            metadataRepository.saveMetadata(
                offlineToken,
                NabuCredentialsMetadata::class.java,
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Completable.complete())
        val responseBody =
            ResponseBody.create(
                MediaType.parse("application/json"),
                "{}"
            )
        whenever(
            nabuDataManager.createBasicUser(
                firstName,
                lastName,
                dateOfBirth.toISO8601DateString(),
                offlineToken.mapFromMetadata()
            )
        ).thenReturn(Completable.error {
            NabuApiException.fromResponseBody(
                Response.error<Unit>(409, responseBody)
            )
        })
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }

    @Test
    fun `onViewReady no data to restore`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error(MetadataNotFoundException("Nabu Token not found")))
        // Act
        subject.onViewReady()
        // Assert
        verifyZeroInteractions(view)
    }

    @Test
    fun `onViewReady restores data to the UI`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val nabuUser = NabuUser(
            firstName = "FIRST_NAME",
            lastName = "LAST_NAME",
            email = null,
            emailVerified = false,
            dob = "2000-09-05",
            mobile = null,
            mobileVerified = false,
            address = null,
            state = UserState.Created,
            kycState = KycState.None,
            updatedAt = "",
            insertedAt = ""
        )
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).restoreUiState(
            eq(nabuUser.firstName!!),
            eq(nabuUser.lastName!!),
            eq("September 05, 2000"),
            any()
        )
    }

    @Test
    fun `onViewReady does not restore data as it's already present`() {
        // Arrange
        subject.firstNameSet = true
        subject.lastNameSet = true
        subject.dateSet = true
        // Act
        subject.onViewReady()
        // Assert
        verifyZeroInteractions(metadataRepository)
        verifyZeroInteractions(nabuDataManager)
    }
}