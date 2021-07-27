package piuk.blockchain.android.scan

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.blockchain.koin.payloadScope
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.FormatsUtil.BCH_PREFIX
import info.blockchain.wallet.util.FormatsUtil.BTC_PREFIX
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.MaybeSubject
import io.reactivex.subjects.SingleSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoTarget
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.BitPayInvoiceTarget
import piuk.blockchain.android.coincore.impl.LunuInvoiceTarget
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.LUNU_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import piuk.blockchain.android.data.api.bitpay.PATH_LUNU_INVOICE
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import java.lang.IllegalArgumentException
import java.security.KeyPair

sealed class ScanResult(
    val isDeeplinked: Boolean
) {
    class HttpUri(
        val uri: String,
        isDeeplinked: Boolean
    ) : ScanResult(isDeeplinked)

    class TxTarget(
        val targets: Set<CryptoTarget>,
        isDeeplinked: Boolean
    ) : ScanResult(isDeeplinked)

    class ImportedWallet(
        val keyPair: KeyPair
    ) : ScanResult(false)

    class SecuredChannelLogin(
        val handshake: String
    ) : ScanResult(false)
}

class QrScanError(val errorCode: ErrorCode, msg: String) : Exception(msg) {
    enum class ErrorCode {
        ScanFailed, // General Purpose Error. The most common case for now until scan gets overhauled
        BitPayScanFailed
    }
}

class QrScanResultProcessor(
    private val bitPayDataManager: BitPayDataManager,
    mwaFeatureFlag: FeatureFlag
) {
    private var isMWAEnabled: Boolean = false

    init {
        val compositeDisposable = mwaFeatureFlag.enabled.observeOn(Schedulers.io()).subscribe(
            { result ->
                isMWAEnabled = result
            },
            {
                isMWAEnabled = false
            }
        )
    }

    fun processScan(scanResult: String, isDeeplinked: Boolean = false): Single<ScanResult> =
        when {
            scanResult.isHttpUri() -> Single.just(ScanResult.HttpUri(scanResult, isDeeplinked))
            scanResult.isBitpayUri() -> parseBitpayInvoice(scanResult)
                .map {
                    ScanResult.TxTarget(setOf(it), isDeeplinked)
                }
            scanResult.isLunuUri() -> parseLunuInvoice(scanResult)
                .map {
                    ScanResult.TxTarget(setOf(it), isDeeplinked)
                }
            isMWAEnabled && scanResult.isJson() -> Single.just(ScanResult.SecuredChannelLogin(scanResult))
            else -> {
                val addressParser: AddressFactory = payloadScope.get()
                addressParser.parse(scanResult)
                    .onErrorResumeNext {
                        Single.error(QrScanError(QrScanError.ErrorCode.ScanFailed, it.message ?: "Unknown reason"))
                    }.map {
                        ScanResult.TxTarget(
                            it.filterIsInstance<CryptoAddress>().toSet(),
                            isDeeplinked
                        )
                    }
            }
        }

    private fun parseBitpayInvoice(bitpayUri: String): Single<CryptoTarget> {
        val cryptoCurrency = bitpayUri.getCryptoCurrencyFromLink()
        return BitPayInvoiceTarget.fromLink(cryptoCurrency, bitpayUri, bitPayDataManager)
            .onErrorResumeNext {
                Single.error(QrScanError(QrScanError.ErrorCode.BitPayScanFailed, it.message ?: "Unknown reason"))
            }
    }

    private fun parseLunuInvoice(bitpayUri: String): Single<CryptoTarget> {
        val cryptoCurrency = bitpayUri.getCryptoCurrencyFromLink()
        return LunuInvoiceTarget.fromLink(cryptoCurrency, bitpayUri, bitPayDataManager)
            .onErrorResumeNext {
                Single.error(QrScanError(QrScanError.ErrorCode.BitPayScanFailed, it.message ?: "Unknown reason"))
            }
    }

    fun disambiguateScan(
        activity: Activity,
        targets: Collection<CryptoTarget>,
        assetResources: AssetResources
    ): Single<CryptoTarget> {
        // TEMP while refactoring - replace with bottom sheet.
        val optionsList = ArrayList(targets)
        val selectList = optionsList.map {
            activity.resources.getString(assetResources.assetNameRes(it.asset))
        }.toTypedArray()

        val subject = SingleSubject.create<CryptoTarget>()

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_currency)
            .setCancelable(true)
            .setSingleChoiceItems(
                selectList,
                -1
            ) { dialog, which ->
                subject.onSuccess(optionsList[which])
                dialog.dismiss()
            }
            .create()
            .show()

        return subject
    }

    fun selectAssetTargetFromScan(
        asset: CryptoCurrency,
        scanResult: ScanResult
    ): Maybe<CryptoAddress> =
        Maybe.just(scanResult)
            .filter { r -> r is ScanResult.TxTarget }
            .map { r ->
                (r as ScanResult.TxTarget).targets
                    .filterIsInstance<CryptoAddress>()
                    .first { a -> a.asset == asset }
            }.onErrorComplete()

    // TODO: Move this into the flow.
    // To not be a hack, this needs the TxTarget interface relationships
    // to be updated so that we can tell an internal target (account) from and external target (address)
    // there is a similar requirement elsewhere in the flow that as a commented workaround.
    @SuppressLint("CheckResult")
    fun selectSourceAccount(
        activity: BlockchainActivity,
        target: CryptoTarget
    ): Maybe<CryptoAccount> {
        // TODO: We currently only support sending to external addresses from a non-custodial
        // account. At some point, this will - maybe - change and we'll have to implement
        // a 'can send from' method on coincore.

        val subject = MaybeSubject.create<CryptoAccount>()

        val asset = target.asset
        val coincore = payloadScope.get<Coincore>()

        coincore[asset].accountGroup(AssetFilter.NonCustodial)
            .subscribeBy(
                onSuccess = {
                    when (it.accounts.size) {
                        1 -> subject.onSuccess(it.accounts[0] as CryptoAccount)
                        0 -> throw IllegalStateException("No account found")
                        else -> showAccountSelectionDialog(
                            activity, subject, Single.just(it.accounts)
                        )
                    }
                },
                onComplete = {
                    subject.onComplete()
                },
                onError = {
                    subject.onError(it)
                }
            )
        return subject
    }

    private fun showAccountSelectionDialog(
        activity: BlockchainActivity,
        subject: MaybeSubject<CryptoAccount>,
        source: Single<SingleAccountList>
    ) {
        val selectionHost = object : AccountSelectSheet.SelectionHost {
            override fun onAccountSelected(account: BlockchainAccount) {
                subject.onSuccess(account as CryptoAccount)
            }

            override fun onSheetClosed() {
                if (!subject.hasValue())
                    subject.onComplete()
            }
        }

        activity.showBottomSheet(
            AccountSelectSheet.newInstance(
                selectionHost,
                source.map { list ->
                    list.map { it as CryptoAccount }
                },
                R.string.select_send_source_title
            )
        )
    }
}

private fun String.isHttpUri(): Boolean = startsWith("http")

private const val bitpayInvoiceUrl = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE/"
private const val lunuInvoiceUrl = "$LUNU_LIVE_BASE$PATH_LUNU_INVOICE"

private fun String.isBitpayUri(): Boolean =
    FormatsUtil.getPaymentRequestUrl(this).contains(bitpayInvoiceUrl)

private fun String.isLunuUri(): Boolean =
    FormatsUtil.getPaymentRequestUrl(this).contains(lunuInvoiceUrl)

private fun String.getCryptoCurrencyFromLink(): CryptoCurrency =
    when {
        this.startsWith(BTC_PREFIX) -> CryptoCurrency.BTC
        this.startsWith(BCH_PREFIX) -> CryptoCurrency.BCH
        else -> throw IllegalArgumentException("$this cannot be mapped to a supported CryptoCurrency")
    }

private fun String.isJson(): Boolean = FormatsUtil.isValidJson(this)