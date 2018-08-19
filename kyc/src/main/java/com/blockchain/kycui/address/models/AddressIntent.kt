package com.blockchain.kycui.address.models

sealed class AddressIntent {
    class FirstLine(val firstLine: String) : AddressIntent()
    class SecondLine(val secondLine: String) : AddressIntent()
    class City(val city: String) : AddressIntent()
    class State(val state: String) : AddressIntent()
    class PostCode(val postCode: String) : AddressIntent()
    class Country(val country: String) : AddressIntent()
}

data class AddressModel(
    val firstLine: String,
    val secondLine: String?,
    val city: String,
    val state: String?,
    val postCode: String,
    val country: String
)
