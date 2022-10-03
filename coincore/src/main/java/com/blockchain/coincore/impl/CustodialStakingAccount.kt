package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StakingAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxSourceState
import com.blockchain.core.interest.domain.model.InterestState
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.staking.domain.model.StakingService
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean

class CustodialStakingAccount(
    override val currency: AssetInfo,
    override val label: String,
    private val internalAccountLabel: String,
    private val stakingService: StakingService,
    private val custodialWalletManager: CustodialWalletManager,
    override val exchangeRates: ExchangeRatesDataManager,
    private val identity: UserIdentity,
    private val kycService: KycService,
) : CryptoAccountBase(), StakingAccount {

    override val baseActions: Set<AssetAction> = emptySet() // Not used by this class

    private val hasFunds = AtomicBoolean(false)

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedError())

    // TODO(dserrano) - STAKING
    /*stakingService.getAddress(currency).map { address ->
            makeExternalAssetAddress(
                asset = currency,
                address = address,
                label = label,
                postTransactions = onTxCompleted
            )
        }*/

    override val onTxCompleted: (TxResult) -> Completable
        get() = { Completable.error(NotImplementedError()) } /*{ txResult ->
            require(txResult.amount is CryptoValue)
            require(txResult is TxResult.HashedTxResult)
            receiveAddress.flatMapCompletable { receiveAddress ->
                custodialWalletManager.createPendingDeposit(
                    crypto = txResult.amount.currency,
                    address = receiveAddress.address,
                    hash = txResult.txId,
                    amount = txResult.amount,
                    product = Product.SAVINGS
                )
            }
        }*/

    override val directions: Set<TransferDirection>
        get() = emptySet()

    override fun requireSecondPassword(): Single<Boolean> =
        Single.just(false)

    override fun matches(other: CryptoAccount): Boolean =
        other is CustodialStakingAccount && other.currency == currency

    // TODO(dserrano) - STAKING
    override val balanceRx: Observable<AccountBalance>
        get() = Observable.just(AccountBalance.zero(currency))

    /* Observable.combineLatest(
            stakingService.getBalanceFor(currency),
            exchangeRates.exchangeRateToUserFiat(currency)
        ) { balance, rate ->
            AccountBalance.from(balance, rate)
        }.doOnNext { hasFunds.set(it.total.isPositive) }
    */

    // TODO(dserrano) - STAKING
    override val activity: Single<ActivitySummaryList>
        get() = Single.error(NotImplementedError())
    /*
        stakingService.getActivity(currency)
            .onErrorReturn { emptyList() }
            .mapList { interestActivity ->
                interestActivityToSummary(asset = currency, interestActivity = interestActivity)
            }
            .filterActivityStates()
            .doOnSuccess {
                setHasTransactions(it.isNotEmpty())
            }
    */

    // TODO(dserrano) - STAKING
    /* private fun stakingActivityToSummary(asset: AssetInfo, stakingActivity: StakingActivity): ActivitySummaryItem =
         CustodialInterestActivitySummaryItem(
             exchangeRates = exchangeRates,
             asset = asset,
             txId = stakingActivity.id,
             timeStampMs = stakingActivity.insertedAt.time,
             value = stakingActivity.value,
             account = this,
             status = stakingActivity.state,
             type = stakingActivity.type,
             confirmations = stakingActivity.extraAttributes?.confirmations ?: 0,
             accountRef = stakingActivity.extraAttributes?.address
                 ?: stakingActivity.extraAttributes?.transferType?.takeIf { it == "INTERNAL" }?.let {
                     internalAccountLabel
                 } ?: "",
             recipientAddress = stakingActivity.extraAttributes?.address ?: ""
         )*/

    /*  private fun Single<ActivitySummaryList>.filterActivityStates(): Single<ActivitySummaryList> {
          return flattenAsObservable { list ->
              list.filter {
                  it is CustodialInterestActivitySummaryItem && displayedStates.contains(it.status)
              }
          }.toList()
      } */

    // No swaps on staking accounts, so just return the activity list unmodified
    override fun reconcileSwaps(
        tradeItems: List<TradeActivitySummaryItem>,
        activity: List<ActivitySummaryItem>
    ): List<ActivitySummaryItem> = activity

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val isDefault: Boolean = false // Default is, presently, only ever a non-custodial account.

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.CAN_TRANSACT)

    // TODO(dserrano) - STAKING - this account type might not even show activity to start off with, ask product
    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = Single.just(setOf(StateAwareAction(ActionState.Available, AssetAction.ViewActivity)))
    /* Single.zip(
            kycService.getHighestApprovedTierLevelLegacy(),
            balance.firstOrError(),
            identity.userAccessForFeature(Feature.DepositInterest)
        ) { tier, balance, depositInterestEligibility ->
            return@zip when (tier) {
                KycTier.BRONZE,
                KycTier.SILVER -> emptySet()
                KycTier.GOLD -> setOf(
                    StateAwareAction(
                        when (depositInterestEligibility) {
                            is FeatureAccess.Blocked -> depositInterestEligibility.toActionState()
                            else -> ActionState.Available
                        },
                        AssetAction.InterestDeposit
                    ),
                    StateAwareAction(
                        if (balance.withdrawable.isPositive) ActionState.Available else ActionState.LockedForBalance,
                        AssetAction.InterestWithdraw
                    ),
                    StateAwareAction(ActionState.Available, AssetAction.ViewStatement),
                    StateAwareAction(ActionState.Available, AssetAction.ViewActivity)
                )
            }.exhaustive
        }
    */

    // TODO(dserrano) - STAKING - unused, check what states Staking activity can be in
    companion object {
        private val displayedStates = setOf(
            InterestState.COMPLETE,
            InterestState.PROCESSING,
            InterestState.PENDING,
            InterestState.MANUAL_REVIEW,
            InterestState.FAILED
        )
    }
}
