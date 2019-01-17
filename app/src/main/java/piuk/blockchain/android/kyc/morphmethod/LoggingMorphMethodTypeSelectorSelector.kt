package piuk.blockchain.android.kyc.morphmethod

import com.blockchain.koin.modules.MorphMethodType
import com.blockchain.koin.modules.MorphMethodTypeSelector
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import io.reactivex.Single

fun MorphMethodTypeSelector.logCalls(eventLogger: EventLogger): MorphMethodTypeSelector =
    LoggingMorphMethodTypeSelectorSelector(this, eventLogger)

private class LoggingMorphMethodTypeSelectorSelector(
    private val inner: MorphMethodTypeSelector,
    private val eventLogger: EventLogger
) : MorphMethodTypeSelector {

    override fun getMorphMethod(): Single<MorphMethodType> {
        eventLogger.logEvent(LoggableEvent.Exchange)
        return inner.getMorphMethod()
    }
}
