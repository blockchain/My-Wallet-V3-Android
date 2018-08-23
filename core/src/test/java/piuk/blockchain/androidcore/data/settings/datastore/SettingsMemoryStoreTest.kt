package piuk.blockchain.androidcore.data.settings.datastore

import info.blockchain.wallet.api.data.Settings
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import com.blockchain.utils.Optional

class SettingsMemoryStoreTest : RxTest() {

    lateinit var subject: SettingsMemoryStore

    @Before
    fun setUp() {
        subject = SettingsMemoryStore()
    }

    @Test
    fun `store settings should return settings`() {
        // Arrange
        val mockSettings: Settings = mock()
        // Act
        val testObserver = subject.store(mockSettings).test()
        // Assert
        testObserver.assertValueAt(0) { it == mockSettings }
    }

    @Test
    fun `getSettings should return Optional None`() {
        // Arrange

        // Act
        val testObserver = subject.getSettings().test()
        // Assert
        testObserver.assertValueAt(0) { it == Optional.None }
    }

    @Test
    fun `getSettings should return settings after store called`() {
        // Arrange
        val mockSettings: Settings = mock()
        subject.store(mockSettings)
        // Act
        val testObserver = subject.getSettings().test()
        // Assert
        testObserver.assertValueAt(0) { it is Optional.Some<Settings> }
        (testObserver.values()[0] as Optional.Some<Settings>).element `should equal` mockSettings
    }

    @Test
    fun `invalidate should clear previously stored settings object`() {
        // Arrange
        val mockSettings: Settings = mock()
        subject.store(mockSettings)
        // Act
        subject.invalidate()
        val testObserver = subject.getSettings().test()
        // Assert
        testObserver.assertValueAt(0) { it == Optional.None }
    }
}