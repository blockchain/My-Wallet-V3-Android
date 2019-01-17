package com.blockchain.kycui.reentry

import com.blockchain.kyc.models.nabu.Address
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.Tiers
import com.blockchain.kyc.models.nabu.UserState
import org.amshove.kluent.`should be`
import org.junit.Test

class ReentryDecisionTest {

    @Test
    fun `if email is unverified - go to email entry`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = false
            )
        ) `should be` ReentryPoint.EmailEntry
    }

    @Test
    fun `if country code is unset - go to country code entry`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = true
            )
        ) `should be` ReentryPoint.CountrySelection
    }

    @Test
    fun `if profile is not set - go to profile`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                )
            )
        ) `should be` ReentryPoint.Profile
    }

    @Test
    fun `if profile is set - go to address`() {
        whereNext(
            createdNabuUser(selected = 1).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.Address
    }

    @Test
    fun `if user is tier 2, and mobile is not verified - go to mobile`() {
        whereNext(
            createdNabuUser(selected = 2, tier = 1).copy(
                mobile = "123456",
                mobileVerified = false
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    @Test
    fun `if user is tier 2, and mobile is verified - go to onfido`() {
        whereNext(
            createdNabuUser(selected = 2, tier = 1).copy(
                mobile = "123456",
                mobileVerified = true
            )
        ) `should be` ReentryPoint.Onfido
    }

    @Test
    fun `if user is tier 0, tier 1 all complete but upgraded go to mobile`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    @Test
    fun `if user is tier 0, upgraded, but no email still go to email`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2)
                .copy(emailVerified = false)
        ) `should be` ReentryPoint.EmailEntry
    }

    @Test
    fun `if user is tier 0, upgraded, but no country still go to country`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2)
                .copy(emailVerified = true)
        ) `should be` ReentryPoint.CountrySelection
    }

    @Test
    fun `if user is tier 0, upgraded but no profile still go to profile`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                )
            )
        ) `should be` ReentryPoint.Profile
    }

    @Test
    fun `if user is tier 0, upgraded then go to mobile`() {
        whereNext(
            createdNabuUser(tier = 0, next = 2).copy(
                email = "abc@def.com",
                emailVerified = true,
                address = Address(
                    line1 = "",
                    line2 = "",
                    city = "",
                    state = "",
                    postCode = "",
                    countryCode = "DE"
                ),
                dob = "dob",
                firstName = "A",
                lastName = "B"
            )
        ) `should be` ReentryPoint.MobileEntry
    }

    private fun whereNext(user: NabuUser) =
        TiersReentryDecision().findReentryPoint(user)

    private fun createdNabuUser(
        selected: Int = 1,
        tier: Int = selected - 1,
        next: Int = selected
    ) =
        emptyNabuUser().copy(
            kycState = KycState.None,
            tiers = Tiers(
                current = tier,
                next = next,
                selected = selected
            )
        )

    private fun emptyNabuUser() =
        NabuUser(
            firstName = null,
            lastName = null,
            email = null,
            emailVerified = false,
            dob = null,
            mobile = null,
            mobileVerified = false,
            address = null,
            state = UserState.None,
            kycState = KycState.None,
            insertedAt = null
        )
}
