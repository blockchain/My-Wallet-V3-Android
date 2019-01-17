package com.blockchain.kyc.status

import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.LimitsJson
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.TierJson
import com.blockchain.kyc.models.nabu.Tiers
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kyc.services.nabu.TierService
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.junit.Test

class KycTiersQueriesTest {

    @Test
    fun `initially kyc is not in progress`() {
        givenUsersTiers(current = 0, selected = 0, next = 0) and
            givenTiersState(tier1State = KycTierState.None, tier2State = KycTierState.None) then {
            isKycInProgress()
        } `should be` false
    }

    @Test
    fun `when the user has selected tier 1 but they have not been verified, it is in progress`() {
        givenUsersTiers(current = 0, selected = 1, next = 1) and
            givenTiersState(tier1State = KycTierState.None, tier2State = KycTierState.None) then {
            isKycInProgress()
        } `should be` true
    }

    @Test
    fun `when the user has selected tier 1 but they have been rejected, it is not in progress`() {
        givenUsersTiers(current = 0, selected = 1, next = 1) and
            givenTiersState(tier1State = KycTierState.Rejected, tier2State = KycTierState.None) then {
            isKycInProgress()
        } `should be` false
    }

    @Test
    fun `when the user has selected tier 1 but they have been upgraded, it is in progress`() {
        givenUsersTiers(current = 0, selected = 1, next = 2) and
            givenTiersState(tier1State = KycTierState.Rejected, tier2State = KycTierState.None) then {
            isKycInProgress()
        } `should be` true
    }

    @Test
    fun `when the user has selected tier 1 and they have been verified, it is not in progress`() {
        givenUsersTiers(current = 1, selected = 1, next = 1) and
            givenTiersState(tier1State = KycTierState.Verified, tier2State = KycTierState.None) then {
            isKycInProgress()
        } `should be` false
    }

    @Test
    fun `when the user has selected tier 1 and they were upgraded and rejected, it is not in progress`() {
        givenUsersTiers(current = 1, selected = 1, next = 2) and
            givenTiersState(tier1State = KycTierState.Verified, tier2State = KycTierState.Rejected) then {
            isKycInProgress()
        } `should be` false
    }

    @Test
    fun `when the user has selected tier 1 and they were upgraded and verified, it is not in progress`() {
        givenUsersTiers(current = 2, selected = 1, next = 2) and
            givenTiersState(tier1State = KycTierState.Verified, tier2State = KycTierState.Verified) then {
            isKycInProgress()
        } `should be` false
    }

    @Test
    fun `when the user has selected tier 2 and they were upgraded and now pending, it is not in progress`() {
        givenUsersTiers(current = 1, selected = 1, next = 2) and
            givenTiersState(tier1State = KycTierState.Verified, tier2State = KycTierState.Pending) then {
            isKycInProgress()
        } `should be` false
    }
}

private infix fun <R> KycTiersQueries.then(function: KycTiersQueries.() -> Single<R>): R =
    function()
        .test()
        .assertComplete()
        .values().single()

private infix fun NabuUser.and(tiersState: TiersJson): KycTiersQueries {
    val nabuDataProvider = mock<NabuDataUserProvider> {
        on { getUser() } `it returns` Single.just(this@and)
    }
    val tiersService = mock<TierService> {
        on { tiers() } `it returns` Single.just(tiersState)
    }
    return KycTiersQueries(nabuDataProvider, tiersService)
}

private fun givenUsersTiers(
    current: Int,
    selected: Int,
    next: Int
) = emptyNabuUser()
    .copy(
        tiers = Tiers(
            selected = selected,
            current = current,
            next = next
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

private fun givenTiersState(tier1State: KycTierState, tier2State: KycTierState) =
    TiersJson(
        tiers = listOf(
            TierJson(
                0,
                "Tier 0",
                state = KycTierState.Verified,
                limits = LimitsJson(
                    currency = "USD",
                    daily = null,
                    annual = null
                )
            ),
            TierJson(
                1,
                "Tier 1",
                state = tier1State,
                limits = LimitsJson(
                    currency = "USD",
                    daily = null,
                    annual = 0.0.toBigDecimal()
                )
            ),
            TierJson(
                2,
                "Tier 2",
                state = tier2State,
                limits = LimitsJson(
                    currency = "USD",
                    daily = 0.0.toBigDecimal(),
                    annual = null
                )
            )
        )
    )
