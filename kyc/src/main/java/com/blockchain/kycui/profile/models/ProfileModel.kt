package com.blockchain.kycui.profile.models

import java.util.Calendar

data class ProfileModel(
    private val firstName: String,
    private val lastName: String,
    private val dateOfBirth: Calendar
)