package piuk.blockchain.android.ui.contacts.list

import android.content.Intent
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.metadata.MetadataNodeFactory
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.shouldEqual
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import com.blockchain.notifications.models.NotificationPayload
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.ArrayList
import java.util.Collections
import java.util.TreeMap
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(
    sdk = intArrayOf(23),
    constants = BuildConfig::class,
    application = BlockchainTestApplication::class
)
@RunWith(RobolectricTestRunner::class)
class ContactsListPresenterTest {

    private lateinit var subject: ContactsListPresenter
    private var mockActivity: ContactsListView = mock()
    private var mockContactsManager: ContactsDataManager = mock()
    private var mockPayloadDataManager: PayloadDataManager = mock()
    private var mockEnvironmentConfig: EnvironmentConfig = mock()
    private val mockRxBus: RxBus = mock()

    @Before
    fun setUp() {
        subject = ContactsListPresenter(mockContactsManager, mockPayloadDataManager, mockEnvironmentConfig, mockRxBus)
        subject.initView(mockActivity)

        whenever(mockEnvironmentConfig.bitcoinNetworkParameters).thenReturn(BitcoinMainNetParams.get())
    }

    @Test
    fun handleLinkSuccessful() {
        // Arrange
        val uri = "METADATA_URI"
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockContactsManager.acceptInvitation(uri)).thenReturn(Observable.just(Contact()))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList()).thenReturn(Observable.empty())
        // Act
        subject.handleLink(uri)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockContactsManager).acceptInvitation(uri)
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).getContactList()
        assertNull(subject.link)
    }

    @Test
    fun handleLinkFailure() {
        // Arrange
        val uri = "METADATA_URI"
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockContactsManager.acceptInvitation(uri)).thenReturn(Observable.error { Throwable() })
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList()).thenReturn(
            Observable.fromIterable(
                Collections.emptyList()
            )
        )
        whenever(mockContactsManager.getContactsWithUnreadPaymentRequests())
            .thenReturn(Observable.fromIterable(listOf<Contact>()))
        // Act
        subject.handleLink(uri)
        // Assert
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verify(mockContactsManager).acceptInvitation(uri)
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).getContactList()
        verify(mockContactsManager).getContactsWithUnreadPaymentRequests()
        verify(mockActivity).onContactsLoaded(ArrayList<ContactsListItem>())
        verify(mockActivity).setUiState(UiState.EMPTY)
        verifyNoMoreInteractions(mockActivity)
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    fun onViewReadyEmitNotificationEvent() {
        // Arrange
        val notificationPayload: NotificationPayload = mock()
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList()).thenReturn(
            Observable.fromIterable(
                Collections.emptyList()
            )
        )
        // Act
        subject.onViewReady()
        notificationObservable.onNext(notificationPayload)
        // Assert
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verify(mockActivity, times(3)).setUiState(UiState.LOADING)
        verify(mockPayloadDataManager).loadNodes()
        verify(mockContactsManager, times(2)).fetchContacts()
        verify(mockContactsManager, times(2)).getContactList()
    }

    @Test
    fun onViewReadyEmitErrorEvent() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList()).thenReturn(
            Observable.fromIterable(
                Collections.emptyList()
            )
        )
        // Act
        subject.onViewReady()
        notificationObservable.onError(Throwable())
        // Assert
        verify(mockRxBus).register(NotificationPayload::class.java)
        verifyNoMoreInteractions(mockRxBus)
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockPayloadDataManager).loadNodes()
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).getContactList()
    }

    @Test
    fun onViewReadyShouldShowSecondPasswordDialog() {
        // Arrange
        val uri = "URI"
        val intent = Intent().apply { putExtra(ContactsListActivity.EXTRA_METADATA_URI, uri) }
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockActivity.pageIntent).thenReturn(intent)
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(mockPayloadDataManager.isDoubleEncrypted).thenReturn(true)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockPayloadDataManager).isDoubleEncrypted
        verify(mockActivity).pageIntent
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).showSecondPasswordDialog()
        verify(mockActivity).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
        subject.link shouldEqual uri
    }

    @Test
    fun onViewReadyShouldInitContacts() {
        // Arrange
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(false))
        whenever(mockPayloadDataManager.isDoubleEncrypted).thenReturn(false)
        whenever(mockPayloadDataManager.generateNodes()).thenReturn(Completable.complete())
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadDataManager.getMetadataNodeFactory())
            .thenReturn(Observable.just(mockNodeFactory))
        whenever(mockNodeFactory.sharedMetadataNode).thenReturn(mock())
        whenever(mockNodeFactory.metadataNode).thenReturn(mock())
        whenever(mockContactsManager.initContactsService(any(), any()))
            .thenReturn(Completable.complete())
        whenever(mockPayloadDataManager.registerMdid())
            .thenReturn(
                Observable.just(
                    ResponseBody.create(
                        MediaType.parse("application/json"),
                        ""
                    )
                )
            )
        whenever(mockContactsManager.publishXpub()).thenReturn(Completable.complete())
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        // There will be other interactions with the mocks, but they are not tested here
        verify(mockPayloadDataManager).isDoubleEncrypted
        verify(mockPayloadDataManager).generateNodes()
        verify(mockPayloadDataManager).getMetadataNodeFactory()
        verify(mockContactsManager).initContactsService(any(), any())
        verify(mockPayloadDataManager).registerMdid()
        verify(mockContactsManager).publishXpub()
    }

    @Test
    fun onViewReadyShouldLoadContactsSuccessfully() {
        // Arrange
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        val id = "ID"
        val contacts = listOf(
            Contact().apply { mdid = "mdid" },
            Contact().apply { mdid = "mdid" },
            Contact().apply {
                mdid = "mdid"
                this.id = id
            })
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockContactsManager.getContactList()).thenReturn(Observable.fromIterable(contacts))
        whenever(mockContactsManager.getContactsWithUnreadPaymentRequests())
            .thenReturn(Observable.fromIterable(listOf(Contact().apply {
                mdid = "mdid"
                this.id = id
            })))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.CONTENT)
        verify(mockActivity).onContactsLoaded(any())
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewReadyShouldLoadContactsEmpty() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList()).thenReturn(Observable.fromIterable(listOf<Contact>()))
        whenever(mockContactsManager.getContactsWithUnreadPaymentRequests())
            .thenReturn(Observable.fromIterable(listOf<Contact>()))
        whenever(mockContactsManager.readInvitationSent(any())).thenReturn(Observable.just(true))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.EMPTY)
        verify(mockActivity).onContactsLoaded(any())
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewReadyShouldFailLoadingRequests() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList())
            .thenReturn(Observable.fromIterable(listOf<Contact>()))
        whenever(mockContactsManager.getContactsWithUnreadPaymentRequests())
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verify(mockActivity).onContactsLoaded(any())
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewReadyShouldFailLoadingContacts() {
        // Arrange
        val notificationObservable = PublishSubject.create<NotificationPayload>()
        whenever(mockRxBus.register(NotificationPayload::class.java)).thenReturn(
            notificationObservable
        )
        whenever(mockPayloadDataManager.loadNodes()).thenReturn(Observable.just(true))
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).pageIntent
        verify(mockActivity, times(2)).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun initContactsServiceShouldThrowDecryptionException() {
        // Arrange
        whenever(mockPayloadDataManager.generateNodes())
            .thenReturn(Completable.error { DecryptionException() })
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadDataManager.getMetadataNodeFactory())
            .thenReturn(Observable.just(mockNodeFactory))
        whenever(mockNodeFactory.sharedMetadataNode).thenReturn(mock())
        whenever(mockNodeFactory.metadataNode).thenReturn(mock())
        whenever(mockContactsManager.initContactsService(any(), any()))
            .thenReturn(Completable.complete())
        // Act
        subject.initContactsService()
        // Assert
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockPayloadDataManager).generateNodes()
        verify(mockPayloadDataManager).getMetadataNodeFactory()
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    fun initContactsServiceShouldThrowException() {
        // Arrange
        whenever(mockPayloadDataManager.generateNodes())
            .thenReturn(Completable.error { Throwable() })
        val mockNodeFactory: MetadataNodeFactory = mock()
        whenever(mockPayloadDataManager.getMetadataNodeFactory())
            .thenReturn(Observable.just(mockNodeFactory))
        whenever(mockNodeFactory.sharedMetadataNode).thenReturn(mock())
        whenever(mockNodeFactory.metadataNode).thenReturn(mock())
        whenever(mockContactsManager.initContactsService(any(), any()))
            .thenReturn(Completable.complete())
        // Act
        subject.initContactsService()
        // Assert
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        verify(mockPayloadDataManager).generateNodes()
        verify(mockPayloadDataManager).getMetadataNodeFactory()
        verifyNoMoreInteractions(mockContactsManager)
    }

    @Test
    fun checkStatusOfPendingContactsSuccess() {
        // Arrange
        whenever(mockContactsManager.readInvitationSent(any()))
            .thenReturn(Observable.just(true))
        whenever(mockContactsManager.getContactList()).thenReturn(Observable.error { Throwable() })
        // Act
        subject.checkStatusOfPendingContacts(listOf(Contact(), Contact(), Contact()))
        // Assert
        verify(mockContactsManager, times(3)).readInvitationSent(any())
        verify(mockContactsManager, times(3)).getContactList()
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity, times(3)).setUiState(UiState.LOADING)
        verify(mockActivity, times(3)).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun refreshContactsFailure() {
        // Arrange
        whenever(mockContactsManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(mockContactsManager.getContactList()).thenReturn(Observable.error { Throwable() })
        // Act
        subject.refreshContacts()
        // Assert
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).getContactList()
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockActivity).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    fun onViewDestroyed() {
        // Arrange

        // Act
        subject.onViewDestroyed()
        // Assert
        verify(mockRxBus).unregister(eq(NotificationPayload::class.java), anyOrNull())
    }

    @Test
    fun setNameOfSender() {
        // Arrange
        val name = "Sender"
        // Act
        subject.setNameOfSender(name)
        // Assert
        assertNotNull(subject.sender)
        subject.sender.name shouldEqual name
    }

    @Test
    fun setNameOfRecipient() {
        // Arrange
        val name = "Recipient"
        // Act
        subject.setNameOfRecipient(name)
        // Assert
        assertNotNull(subject.recipient)
        subject.recipient.name shouldEqual name
    }

    @Test
    fun clearContactNames() {
        // Arrange
        // Act
        subject.clearContactNames()
        // Assert
        assertNull(subject.sender)
        assertNull(subject.recipient)
    }

    @Test
    fun createLink() {
        // Arrange
        val senderName = "SENDER_NAME"
        val recipientName = "RECIPIENT_NAME"
        val sender = Contact().apply {
            name = senderName
            invitationSent = ""
        }
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.sender = sender
            this.recipient = recipient
        }
        whenever(mockContactsManager.publishXpub())
            .thenReturn(Completable.complete())
        whenever(mockContactsManager.createInvitation(sender, recipient))
            .thenReturn(Observable.just(sender))
        // Act
        subject.createLink()
        // Assert

        verify(mockContactsManager).publishXpub()
        verify(mockContactsManager).createInvitation(sender, recipient)
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).onLinkGenerated(any())
        verifyNoMoreInteractions(mockActivity)
        assertNotNull(subject.uri)
    }

    @Test
    fun createLinkFailure() {
        // Arrange
        val senderName = "SENDER_NAME"
        val recipientName = "RECIPIENT_NAME"
        val sender = Contact().apply { name = senderName }
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.sender = sender
            this.recipient = recipient
        }
        whenever(mockContactsManager.publishXpub())
            .thenReturn(Completable.complete())
        whenever(mockContactsManager.createInvitation(sender, recipient))
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.createLink()
        // Assert
        verify(mockContactsManager).publishXpub()
        verify(mockContactsManager).createInvitation(sender, recipient)
        verifyNoMoreInteractions(mockContactsManager)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(mockActivity)
        assertNull(subject.uri)
    }

    @Test
    fun createLinkPreexistingUri() {
        // Arrange
        val uri = "URI"
        val recipientName = "RECIPIENT_NAME"
        val recipient = Contact().apply { name = recipientName }
        subject.uri = uri
        subject.apply { this.recipient = recipient }
        // Act
        subject.createLink()
        // Assert
        verify(mockActivity).onLinkGenerated(any())
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockContactsManager)
    }

    @Test
    fun onDeleteContactConfirmed() {
        // Arrange
        val recipientName = "RECIPIENT_NAME"
        val recipient = Contact().apply { name = recipientName }
        subject.apply {
            this.recipient = recipient
        }
        subject.contactList = TreeMap()
        subject.contactList[recipient.id] = recipient

        whenever(mockContactsManager.removeContact(recipient))
            .thenReturn(Completable.complete())
        // Act
        subject.onDeleteContactConfirmed(recipient.id)
        // Assert
        verify(mockContactsManager).removeContact(recipient)
        verify(mockActivity).showProgressDialog()
        verify(mockActivity).dismissProgressDialog()
        verify(mockActivity).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(mockActivity).setUiState(UiState.LOADING)
        verify(mockContactsManager).fetchContacts()
        verify(mockContactsManager).getContactList()
        verifyNoMoreInteractions(mockActivity)
    }
}
