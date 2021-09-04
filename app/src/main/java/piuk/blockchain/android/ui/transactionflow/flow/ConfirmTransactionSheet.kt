package piuk.blockchain.android.ui.transactionflow.flow

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.core.price.ExchangeRates
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import org.koin.android.ext.android.inject
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.impl.LunuInvoiceTarget
import piuk.blockchain.android.databinding.DialogTxFlowConfirmBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.flow.adapter.ConfirmTransactionDelegateAdapter
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import timber.log.Timber

class ConfirmTransactionSheet : TransactionFlowSheet<DialogTxFlowConfirmBinding>() {

    private val stringUtils: StringUtils by inject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val prefs: CurrencyPrefs by scopedInject()
    private val mapper: TxConfirmReadOnlyMapperCheckout by scopedInject()
    private val customiser: TransactionConfirmationCustomisations by inject()

    private var headerSlot: TxFlowWidget? = null

    private val listAdapter: ConfirmTransactionDelegateAdapter by lazy {
        ConfirmTransactionDelegateAdapter(
            model = model,
            stringUtils = stringUtils,
            activityContext = requireActivity(),
            mapper = mapper,
            selectedCurrency = prefs.selectedFiatCurrency,
            exchangeRates = exchangeRates
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTxFlowConfirmBinding =
        DialogTxFlowConfirmBinding.inflate(inflater, container, false)

    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == TransactionStep.CONFIRM_DETAIL)

        // We _should_ always have a pending Tx when we get here
        newState.pendingTx?.let {
            listAdapter.items = newState.pendingTx.confirmations.toList()
            listAdapter.notifyDataSetChanged()
        }

        with(binding) {
            confirmCtaButton.text = customiser.confirmCtaText(newState)
            confirmSheetTitle.text = customiser.confirmTitle(newState)
            confirmCtaButton.isEnabled = newState.nextEnabled
            confirmSheetBack.visibleIf { newState.canGoBack }

            if (customiser.confirmDisclaimerVisibility(newState.action)) {
                confirmDisclaimer.apply {
                    text = customiser.confirmDisclaimerBlurb(newState, requireContext())
                    visible()
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
            initialiseHeaderSlotIfNeeded(newState)
        }

        headerSlot?.update(newState)
        cacheState(newState)
    }

    private fun DialogTxFlowConfirmBinding.initialiseHeaderSlotIfNeeded(state: TransactionState) {
        if (headerSlot == null) {
            headerSlot = customiser.confirmInstallHeaderView(
                requireContext(),
                confirmHeaderSlot,
                state
            ).apply {
                initControl(model, customiser, analyticsHooks)
            }
        }
    }

    override fun initControls(binding: DialogTxFlowConfirmBinding) {
        binding.confirmCtaButton.setOnClickListener { onCtaClick() }

        with(binding.confirmDetailsList) {
            addItemDecoration(BlockchainListDividerDecor(requireContext()))

            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = listAdapter
        }

        binding.confirmSheetBack.setOnClickListener {
            model.process(TransactionIntent.ReturnToPreviousStep)
        }

        model.process(TransactionIntent.ValidateTransaction)
    }

    private fun onCtaClick() {
        analyticsHooks.onConfirmationCtaClick(state)
        model.process(TransactionIntent.ExecuteTransaction)
    }
}
