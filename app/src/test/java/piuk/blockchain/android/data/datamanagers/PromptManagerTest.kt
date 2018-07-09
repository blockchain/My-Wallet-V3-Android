package piuk.blockchain.android.data.datamanagers

import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.wallet.api.data.Settings
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil

class PromptManagerTest : RxTest() {

    private lateinit var subject: PromptManager
    private val mockPrefsUtil: PrefsUtil = mock()
    private val mockPayloadDataManager: PayloadDataManager = mock()
    private val mockTransactionListDataManager: TransactionListDataManager = mock()
    private val mockSettings: Settings = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject =
            PromptManager(mockPrefsUtil, mockPayloadDataManager, mockTransactionListDataManager)
    }

    @Test
    fun getPreLoginPrompts() {
        // Arrange
        val context: Context = mock()
        // Act
        val testObserver = subject.getPreLoginPrompts(context).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun getCustomPrompts() {
        // Arrange
        val context: Context = mock()
        // Act
        val testObserver = subject.getCustomPrompts(context, mockSettings).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }
}