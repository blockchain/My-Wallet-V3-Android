package piuk.blockchain.android.cards

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.CompleteCardActivation
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import timber.log.Timber

class CardModel(
    uiScheduler: Scheduler,
    currencyPrefs: CurrencyPrefs,
    private val interactor: SimpleBuyInteractor,
    private val prefs: SimpleBuyPrefs,
    private val cardActivator: CardActivator,
    private val json: Json,
    val environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<CardState, CardIntent>(
    initialState = prefs.cardState()?.run {
        json.decodeFromString<CardState>(this)
    } ?: CardState(fiatCurrency = currencyPrefs.selectedFiatCurrency),
    uiScheduler = uiScheduler,
    environmentConfig = environmentConfig,
    remoteLogger = remoteLogger
) {

    override fun performAction(previousState: CardState, intent: CardIntent): Disposable? =
        when (intent) {
            is CardIntent.AddNewCard -> handleAddNewCard(intent, previousState)
            is CardIntent.ActivateCard -> activateCard(intent)
            is CardIntent.CheckCardStatus -> checkCardStatus(previousState)
            CardIntent.LoadLinkedCards -> loadLinkedCards()
            is CardIntent.CheckProviderFailureRate -> checkCardFailureRate(intent.cardNumber)
            else -> null
        }

    private fun checkCardFailureRate(binNumber: String) =
        interactor.checkNewCardRejectionRate(binNumber)
            .subscribeBy(
                onSuccess = { state ->
                    process(CardIntent.UpdateCardRejectionState(state))
                },
                onError = {
                    // if the check fails, allow the user to go through
                    process(CardIntent.UpdateCardRejectionState(CardRejectionState.NotRejected))
                }
            )

    private fun loadLinkedCards() =
        interactor.loadLinkedCards()
            .subscribeBy(
                onSuccess = {
                    process(CardIntent.LinkedCardsLoaded(it))
                },
                onError = {
                    Timber.e("Error loading linked cards ${it.message}")
                }
            )

    private fun handleAddNewCard(
        intent: CardIntent.AddNewCard,
        previousState: CardState
    ) = interactor.addNewCard(
        intent.cardData,
        previousState.fiatCurrency,
        previousState.billingAddress
            ?: throw IllegalStateException("No billing address was provided")
    )
        .doOnSubscribe {
            process(CardIntent.UpdateRequestState(CardRequestStatus.Loading))
        }.subscribeBy(
            onSuccess = { card ->
                process(
                    CardIntent.ActivateCard(
                        cardId = card.cardId,
                        card = intent.cardData
                    )
                )
                process(CardIntent.UpdateCardId(card.cardId))
            },
            onError = {
                process(
                    CardIntent.UpdateRequestState(CardRequestStatus.Error(it.toCardError(CardError.CREATION_FAILED)))
                )
            }
        )

    private fun activateCard(intent: CardIntent.ActivateCard) = cardActivator.activateCard(
        intent.card,
        intent.cardId
    )
        .doOnSubscribe {
            process(CardIntent.UpdateRequestState(CardRequestStatus.Loading))
        }.subscribeBy(
            onSuccess = {
                process(
                    CardIntent.AuthoriseCard(
                        credentials = it.toCardAcquirerCredentials()
                    )
                )
            },
            onError = {
                process(
                    CardIntent.UpdateRequestState(CardRequestStatus.Error(it.toCardError(CardError.ACTIVATION_FAIL)))
                )
            }
        )

    private fun Throwable.toCardError(defaultError: CardError): CardError {
        return if (this is NabuApiException) {
            when (this.getErrorCode()) {
                NabuErrorCodes.InsufficientCardFunds -> CardError.INSUFFICIENT_CARD_BALANCE
                NabuErrorCodes.CardBankDeclined -> CardError.CARD_BANK_DECLINED
                NabuErrorCodes.CardDuplicate -> CardError.CARD_DUPLICATE
                NabuErrorCodes.CardBlockchainDecline -> CardError.CARD_BLOCKCHAIN_DECLINED
                NabuErrorCodes.CardAcquirerDecline -> CardError.CARD_ACQUIRER_DECLINED
                NabuErrorCodes.CardPaymentNotSupported -> CardError.CARD_PAYMENT_NOT_SUPPORTED
                NabuErrorCodes.CardCreateFailed -> CardError.CARD_CREATED_FAILED
                NabuErrorCodes.CardPaymentFailed -> CardError.CARD_PAYMENT_FAILED
                NabuErrorCodes.CardCreateAbandoned -> CardError.CARD_CREATED_ABANDONED
                NabuErrorCodes.CardCreateExpired -> CardError.CARD_CREATED_EXPIRED
                NabuErrorCodes.CardCreateBankDeclined -> CardError.CARD_CREATE_BANK_DECLINED
                NabuErrorCodes.CardCreateDebitOnly -> CardError.CARD_CREATE_DEBIT_ONLY
                NabuErrorCodes.CardPaymentDebitOnly -> CardError.CARD_PAYMENT_DEBIT_ONLY
                NabuErrorCodes.CardCreateNoToken -> CardError.CARD_CREATE_NO_TOKEN
                NabuErrorCodes.CardLimitReached -> CardError.CARD_LIMIT_REACHED
                else -> defaultError
            }
        } else {
            defaultError
        }
    }

    private fun checkCardStatus(previousState: CardState) = interactor.pollForCardStatus(
        previousState.cardId
            ?: throw IllegalStateException("No card ID was provided")
    )
        .doOnSubscribe {
            process(CardIntent.UpdateRequestState(CardRequestStatus.Loading))
        }
        .subscribeBy(
            onSuccess = {
                process(it)
                if (it.cardDetails.status == CardStatus.ACTIVE) {
                    process(
                        CardIntent.UpdateRequestState(
                            CardRequestStatus.Success(
                                it.cardDetails
                            )
                        )
                    )
                } else {
                    process(
                        CardIntent.UpdateRequestState(
                            CardRequestStatus.Error(
                                if (it.cardDetails.status == CardStatus.PENDING) CardError.PENDING_AFTER_POLL
                                else CardError.LINK_FAILED
                            )
                        )
                    )
                }
            },
            onError = {
                process(CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.PENDING_AFTER_POLL)))
            }
        )

    private fun CompleteCardActivation.toCardAcquirerCredentials() = when (this) {
        is CompleteCardActivation.EverypayCompleteCardActivationDetails ->
            CardAcquirerCredentials.Everypay(paymentLink, exitLink)
        is CompleteCardActivation.StripeCardActivationDetails ->
            CardAcquirerCredentials.Stripe(
                apiKey,
                clientSecret
            )
        is CompleteCardActivation.CheckoutCardActivationDetails -> {
            CardAcquirerCredentials.Checkout(
                apiKey,
                paymentLink,
                exitLink
            )
        }
    }

    override fun onStateUpdate(s: CardState) {
        prefs.updateCardState(json.encodeToString(s))
    }
}
