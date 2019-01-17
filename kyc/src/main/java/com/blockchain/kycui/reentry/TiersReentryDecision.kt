package com.blockchain.kycui.reentry

import com.blockchain.kyc.models.nabu.NabuUser

class TiersReentryDecision : ReentryDecision {

    override fun findReentryPoint(user: NabuUser): ReentryPoint {

        if (user.tiers?.current == 0) {

            if (user.emailVerified != true) {
                return ReentryPoint.EmailEntry
            }

            if (user.address?.countryCode == null) {
                return ReentryPoint.CountrySelection
            }

            if (user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank() || user.dob.isNullOrBlank()) {
                return ReentryPoint.Profile
            }

            if (user.tiers.next == 1) {
                return ReentryPoint.Address
            }
        }

        if (!user.mobileVerified) {
            return ReentryPoint.MobileEntry
        }

        return ReentryPoint.Onfido
    }
}
