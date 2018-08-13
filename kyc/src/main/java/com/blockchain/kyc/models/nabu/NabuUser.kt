package com.blockchain.kyc.models.nabu

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

data class NabuUser(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val mobile: String?,
    val mobileVerified: Boolean,
    val address: Address?,
    val state: UserState,
    val kycState: KycState
)

data class Address(
    val city: String?,
    val line1: String?,
    val line2: String?,
    val state: String?,
    val country: String?,
    val postCode: String?
)

sealed class KycState {
    object None : KycState()
    object Pending : KycState()
    object Rejected : KycState()
    object Expired : KycState()
    object Verified : KycState()
}

sealed class UserState {
    object None : UserState()
    object Created : UserState()
    object Active : UserState()
    object Blocked : UserState()
}

class KycStateAdapter {

    @FromJson
    fun fromJson(input: String): KycState = when (input) {
        NONE -> KycState.None
        PENDING -> KycState.Pending
        REJECTED -> KycState.Rejected
        EXPIRED -> KycState.Expired
        VERIFIED -> KycState.Verified
        else -> throw JsonDataException("Unknown KYC State: $input, unsupported data type")
    }

    @ToJson
    fun toJson(kycState: KycState): String = when (kycState) {
        KycState.None -> NONE
        KycState.Pending -> PENDING
        KycState.Rejected -> REJECTED
        KycState.Expired -> EXPIRED
        KycState.Verified -> VERIFIED
    }

    private companion object {
        private const val NONE = "NONE"
        private const val PENDING = "PENDING"
        private const val REJECTED = "REJECTED"
        private const val EXPIRED = "EXPIRED"
        private const val VERIFIED = "VERIFIED"
    }
}

class UserStateAdapter {

    @FromJson
    fun fromJson(input: String): UserState = when (input) {
        NONE -> UserState.None
        CREATED -> UserState.Created
        ACTIVE -> UserState.Active
        BLOCKED -> UserState.Blocked
        else -> throw JsonDataException("Unknown User State: $input, unsupported data type")
    }

    @ToJson
    fun toJson(userState: UserState): String = when (userState) {
        UserState.None -> NONE
        UserState.Created -> CREATED
        UserState.Active -> ACTIVE
        UserState.Blocked -> BLOCKED
    }

    private companion object {
        private const val NONE = "NONE"
        private const val CREATED = "CREATED"
        private const val ACTIVE = "ACTIVE"
        private const val BLOCKED = "BLOCKED"
    }
}