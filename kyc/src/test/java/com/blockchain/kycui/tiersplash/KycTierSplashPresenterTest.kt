package com.blockchain.kycui.tiersplash

import androidx.navigation.NavDirections
import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.LimitsJson
import com.blockchain.kyc.models.nabu.TierJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kyc.services.nabu.TierUpdater
import com.blockchain.kycui.reentry.KycNavigator
import com.blockchain.testutils.usd
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.kyc.KycNavXmlDirections
import piuk.blockchain.kyc.R

class KycTierSplashPresenterTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
    }

    @Test
    fun `on tier1 selected`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(email()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier1Selected()
        verify(view).navigateTo(email(), 1)
        verify(tierUpdater).setUserTier(1)
    }

    @Test
    fun `on tier1 selected - error setting tier`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenUnableToSetTier()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(email()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier1Selected()
        verify(tierUpdater).setUserTier(1)
        verify(view, never()).navigateTo(any(), any())
        verify(view).showErrorToast(R.string.kyc_non_specific_server_error)
    }

    @Test
    fun `on tier1 selected but tier 1 is verified`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(
            tierUpdater,
            givenTiers(
                tiers(
                    KycTierState.Verified to 1000.usd(),
                    KycTierState.None to 25000.usd()
                )
            ),
            givenRedirect(mobile())
        ).also {
            it.initView(view)
            it.onViewResumed()
        }.tier1Selected()
        verify(view, never()).navigateTo(any(), any())
        verify(tierUpdater, never()).setUserTier(any())
    }

    @Test
    fun `on tier2 selected`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(veriff()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier2Selected()
        verify(view).navigateTo(veriff(), 2)
        verify(tierUpdater).setUserTier(2)
    }

    @Test
    fun `on tier2 selected - error setting tier`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenUnableToSetTier()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(veriff()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier2Selected()
        verify(tierUpdater).setUserTier(2)
        verify(view, never()).navigateTo(any(), any())
        verify(view).showErrorToast(R.string.kyc_non_specific_server_error)
    }

    @Test
    fun `on tier2 selected but tier 2 is verified`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(
            tierUpdater,
            givenTiers(
                tiers(
                    KycTierState.None to 1000.usd(),
                    KycTierState.Verified to 25000.usd()
                )
            ),
            mock()
        ).also {
            it.initView(view)
            it.onViewResumed()
        }.tier2Selected()
        verify(view, never()).navigateTo(any(), any())
        verify(tierUpdater, never()).setUserTier(any())
    }

    private fun givenTierUpdater(): TierUpdater =
        mock {
            on { setUserTier(any()) } `it returns` Completable.complete()
        }

    private fun givenUnableToSetTier(): TierUpdater =
        mock {
            on { setUserTier(any()) } `it returns` Completable.error(Throwable())
        }
}

private fun givenTiers(tiers: TiersJson? = null): TierService =
    mock {
        on { tiers() } `it returns` Single.just(
            tiers ?: tiers(
                KycTierState.None to 1000.usd(),
                KycTierState.None to 25000.usd()
            )
        )
    }

fun tiers(tier1: Pair<KycTierState, FiatValue>, tier2: Pair<KycTierState, FiatValue>) =
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
                state = tier1.first,
                limits = LimitsJson(
                    currency = tier1.second.currencyCode,
                    daily = null,
                    annual = tier1.second.toBigDecimal()
                )
            ),
            TierJson(
                2,
                "Tier 2",
                state = tier2.first,
                limits = LimitsJson(
                    currency = tier2.second.currencyCode,
                    daily = null,
                    annual = tier2.second.toBigDecimal()
                )
            )
        )
    )

private fun email(): NavDirections = KycNavXmlDirections.ActionStartEmailVerification()
private fun mobile(): NavDirections = KycNavXmlDirections.ActionStartMobileVerification("DE")
private fun veriff(): NavDirections = KycNavXmlDirections.ActionStartVeriff("DE")

private fun givenRedirect(email: NavDirections): KycNavigator =
    mock {
        on {
            findNextStep()
        } `it returns` Single.just(email)
    }