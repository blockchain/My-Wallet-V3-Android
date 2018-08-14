package com.blockchain.kyc.models.nabu

data class NabuCountryResponse(
    val code: String,
    val name: String,
    val regions: List<String>,
    val scopes: List<String>
)

// TODO: There will likely be more scopes in future
enum class Scope(val value: String?) {
    Kyc("kyc"),
    None(null);
}