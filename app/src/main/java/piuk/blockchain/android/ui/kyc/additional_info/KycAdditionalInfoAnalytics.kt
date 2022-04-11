package piuk.blockchain.android.ui.kyc.additional_info

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

object KycAdditionalInfoViewed : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_ADDITIONAL_INFO_VIEWED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}

object KycAdditionalInfoSubmitted : AnalyticsEvent {
    override val event: String = AnalyticsNames.KYC_ADDITIONAL_INFO_SUBMITTED.eventName
    override val params: Map<String, Serializable> = emptyMap()
}