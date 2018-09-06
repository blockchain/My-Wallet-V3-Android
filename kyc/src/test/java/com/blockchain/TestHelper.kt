package com.blockchain

import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState

fun getBlankNabuUser(kycState: KycState = KycState.None): NabuUser = NabuUser(
    "",
    "",
    "",
    "",
    "",
    false,
    null,
    UserState.None,
    kycState,
    "",
    ""
)