package com.blockchain.kycui

import android.support.v4.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment

fun Fragment.navigate(directions: NavDirections) {
    NavHostFragment
        .findNavController(this)
        .navigate(directions)
}
