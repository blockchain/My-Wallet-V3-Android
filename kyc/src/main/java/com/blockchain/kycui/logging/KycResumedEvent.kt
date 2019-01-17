package com.blockchain.kycui.logging

import com.blockchain.kycui.reentry.ReentryPoint
import com.blockchain.logging.CustomEventBuilder

internal class KycResumedEvent(entryPoint: ReentryPoint) : CustomEventBuilder("User Resumed KYC flow") {

    init {
        putCustomAttribute("User resumed KYC", entryPoint.entryPoint)
    }
}
