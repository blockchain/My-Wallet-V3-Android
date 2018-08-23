package com.blockchain.kycui.navhost.models

enum class KycStep(val relativeValue: Int) {
    SplashPage(0),
    CountrySelection(10),
    ProfilePage(10),
    FirstName(5),
    LastName(5),
    Birthday(5),
    AddressPage(10),
    AddressFirstLine(5),
    AptNameOrNumber(5),
    City(5),
    State(5),
    ZipCode(5),
    MobileNumberPage(10),
    MobileNumberEntered(10),
    MobileVerifiedPage(10),
    VerificationCodeEntered(10),
    OnfidoSplashPage(5),
    CompletePage(30)
}