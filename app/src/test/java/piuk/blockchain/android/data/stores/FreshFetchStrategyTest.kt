package piuk.blockchain.android.data.stores

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.mock
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.datastores.persistentstore.FreshFetchStrategy
import com.blockchain.data.datastores.PersistentStore

class FreshFetchStrategyTest : RxTest() {

    lateinit var subject: FreshFetchStrategy<String>
    lateinit var webSource: Observable<String>
    val memoryStore: PersistentStore<String> = mock()

    @Test
    fun `fetch should store in memory`() {
        val value = "VALUE"
        webSource = Observable.just(value)
        whenever(memoryStore.store(value)).thenReturn(Observable.just(value))
        subject = FreshFetchStrategy(
            webSource,
            memoryStore
        )
        // Act
        val testObserver = subject.fetch().test()
        // Assert
        verify(memoryStore).store(value)
        testObserver.values()[0] `should equal to` value
    }
}