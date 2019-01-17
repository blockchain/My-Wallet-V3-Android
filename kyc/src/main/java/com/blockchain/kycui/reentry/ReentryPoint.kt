package com.blockchain.kycui.reentry

enum class ReentryPoint(val entryPoint: String) {
    EmailEntry("Email Entry"),
    CountrySelection("Country Selection"),
    Profile("Profile Entry"),
    Address("Address Entry"),
    MobileEntry("Mobile Entry"),
    Onfido("Onfido Splash")
}
