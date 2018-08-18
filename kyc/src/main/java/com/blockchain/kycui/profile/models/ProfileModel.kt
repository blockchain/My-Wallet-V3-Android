package com.blockchain.kycui.profile.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Calendar

@Parcelize
data class ProfileModel(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: Calendar,
    val countryCode: String
) : Parcelable