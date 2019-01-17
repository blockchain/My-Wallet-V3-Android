package piuk.blockchain.android.kyc.morphmethod

import com.blockchain.koin.modules.MorphMethodType
import com.blockchain.koin.modules.MorphMethodTypeSelector
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.mock
import org.junit.Test

class LoggingMorphMethodTypeSelectorSelectorTest {

    @Test
    fun `logs calls`() {
        val inner: MorphMethodTypeSelector = mock()
        val eventLogger = mock<EventLogger>()
        val logCalls = inner.logCalls(eventLogger)
        verify(eventLogger, never()).logEvent(LoggableEvent.Exchange)
        logCalls.getMorphMethod()
        verify(eventLogger).logEvent(LoggableEvent.Exchange)
    }

    @Test
    fun `passes result from inner`() {
        val innerResult = Single.just(MorphMethodType.HomeBrew)
        val inner: MorphMethodTypeSelector = mock {
            on { getMorphMethod() } `it returns` innerResult
        }
        val eventLogger = mock<EventLogger>()
        inner.logCalls(eventLogger).getMorphMethod() `should be` innerResult
    }
}
