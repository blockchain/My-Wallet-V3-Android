package piuk.blockchain.android.kyc.morphmethod

import com.blockchain.koin.modules.MorphMethodType
import com.blockchain.koin.modules.MorphMethodTypeSelector
import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.Tiers
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.kycui.settings.SettingsKycState
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Test

class TiersMorphMethodTypeSelectorSelectorTest {

    private val kycStatusHelper: KycStatusHelper = mock()
    private val nabuDataUserProvider: NabuDataUserProvider = mock()

    @Test
    fun `on hidden, should throw`() {
        givenShouldBeHidden()
        givenUserOnTier(1)

        tiersSelector()
            .getMorphMethod()
            .test()
            .assertError(IllegalStateException::class.java)
    }

    @Test
    fun `on verified, should return Homebrew`() {
        givenUserIsVerified()
        givenUserOnTier(1)

        tiersSelector()
            .getMorphMethod()
            .test()
            .assertValue(MorphMethodType.HomeBrew)
    }

    @Test
    fun `unverified, should return Kyc`() {
        givenUserIs(SettingsKycState.Unverified)
        givenUserOnTier(0)

        tiersSelector().getMorphMethod()
            .test()
            .assertValue(MorphMethodType.Kyc)
    }

    @Test
    fun `unverified on tier 2, but user on tier 1, should return HomeBrew`() {
        givenUserIs(SettingsKycState.Unverified)
        givenUserOnTier(1)

        tiersSelector().getMorphMethod()
            .test()
            .assertValue(MorphMethodType.HomeBrew)
    }

    private fun givenUserIsVerified() {
        givenUserIs(SettingsKycState.Verified)
    }

    private fun givenUserIs(kycState: SettingsKycState) {
        whenever(kycStatusHelper.getSettingsKycState())
            .thenReturn(Single.just(kycState))
    }

    private fun givenShouldBeHidden() {
        whenever(kycStatusHelper.getSettingsKycState())
            .thenReturn(Single.just(SettingsKycState.Hidden))
    }

    private fun givenUserOnTier(tier: Int) {
        whenever(nabuDataUserProvider.getUser())
            .thenReturn(Single.just(userOnTier(tier)))
    }

    private fun tiersSelector(): MorphMethodTypeSelector =
        TiersMorphMethodTypeSelectorSelector(
            kycStatusHelper,
            nabuDataUserProvider
        )
}

private fun userOnTier(tier: Int) =
    NabuUser(
        firstName = "",
        lastName = "",
        email = "",
        emailVerified = false,
        dob = "",
        mobile = "",
        mobileVerified = false,
        address = null,
        state = UserState.Created,
        kycState = KycState.None,
        insertedAt = "",
        updatedAt = "",
        tags = null,
        tiers = Tiers(
            current = tier,
            next = null,
            selected = null
        )
    )
