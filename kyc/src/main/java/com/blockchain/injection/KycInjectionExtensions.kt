package com.blockchain.injection

import android.support.v4.app.Fragment

internal fun Fragment.getKycComponent(): KycComponent =
    (requireActivity().application as KycDependencyGraph).getKycComponent()