package piuk.blockchain.android.ui.dashboard.models

import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent

data class OnboardingModel(
        val pagerContent: List<OnboardingPagerContent>,
        val dismissOnboarding: () -> Unit,
        val onboardingComplete: () -> Unit,
        val onboardingNotComplete: () -> Unit
)