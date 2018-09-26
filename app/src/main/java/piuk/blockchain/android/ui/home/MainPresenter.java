package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.util.Pair;
import com.blockchain.kycui.settings.KycStatusHelper;
import com.blockchain.notifications.models.NotificationPayload;
import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.data.FeeOptions;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.payload.PayloadManagerWiper;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.androidcore.data.fees.FeeDataManager;
import piuk.blockchain.android.data.datamanagers.PromptManager;
import piuk.blockchain.androidcore.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.dashboard.DashboardPresenter;
import piuk.blockchain.android.ui.home.models.MetadataEvent;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager;
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager;
import piuk.blockchain.androidbuysell.services.ExchangeService;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager;
import piuk.blockchain.androidcore.data.contacts.models.ContactsEvent;
import piuk.blockchain.androidcore.data.currency.CurrencyState;
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager;
import piuk.blockchain.androidcore.data.metadata.MetadataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.data.settings.SettingsDataManager;
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager;
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcoreui.ui.base.BasePresenter;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AppUtil;
import piuk.blockchain.androidcoreui.utils.logging.Logging;
import piuk.blockchain.androidcoreui.utils.logging.SecondPasswordEvent;
import timber.log.Timber;

import javax.inject.Inject;
import java.util.NoSuchElementException;

public class MainPresenter extends BasePresenter<MainView> {

    private Observable<NotificationPayload> notificationObservable;
    private PrefsUtil prefs;
    private AppUtil appUtil;
    private AccessState accessState;
    private PayloadManagerWiper payloadManagerWiper;
    private PayloadDataManager payloadDataManager;
    private ContactsDataManager contactsDataManager;
    private Context applicationContext;
    private SettingsDataManager settingsDataManager;
    private BuyDataManager buyDataManager;
    private DynamicFeeCache dynamicFeeCache;
    private ExchangeRateDataManager exchangeRateFactory;
    private RxBus rxBus;
    private FeeDataManager feeDataManager;
    private PromptManager promptManager;
    private EthDataManager ethDataManager;
    private BchDataManager bchDataManager;
    private CurrencyState currencyState;
    private WalletOptionsDataManager walletOptionsDataManager;
    private MetadataManager metadataManager;
    private StringUtils stringUtils;
    private ShapeShiftDataManager shapeShiftDataManager;
    private EnvironmentConfig environmentSettings;
    private CoinifyDataManager coinifyDataManager;
    private ExchangeService exchangeService;
    private KycStatusHelper kycStatusHelper;

    @Inject
    MainPresenter(PrefsUtil prefs,
                  AppUtil appUtil,
                  AccessState accessState,
                  PayloadManagerWiper payloadManagerWiper,
                  PayloadDataManager payloadDataManager,
                  ContactsDataManager contactsDataManager,
                  Context applicationContext,
                  SettingsDataManager settingsDataManager,
                  BuyDataManager buyDataManager,
                  DynamicFeeCache dynamicFeeCache,
                  ExchangeRateDataManager exchangeRateFactory,
                  RxBus rxBus,
                  FeeDataManager feeDataManager,
                  PromptManager promptManager,
                  EthDataManager ethDataManager,
                  BchDataManager bchDataManager,
                  CurrencyState currencyState,
                  WalletOptionsDataManager walletOptionsDataManager,
                  MetadataManager metadataManager,
                  StringUtils stringUtils,
                  ShapeShiftDataManager shapeShiftDataManager,
                  EnvironmentConfig environmentSettings,
                  CoinifyDataManager coinifyDataManager,
                  ExchangeService exchangeService,
                  KycStatusHelper kycStatusHelper) {

        this.prefs = prefs;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.payloadManagerWiper = payloadManagerWiper;
        this.payloadDataManager = payloadDataManager;
        this.contactsDataManager = contactsDataManager;
        this.applicationContext = applicationContext;
        this.settingsDataManager = settingsDataManager;
        this.buyDataManager = buyDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
        this.exchangeRateFactory = exchangeRateFactory;
        this.rxBus = rxBus;
        this.feeDataManager = feeDataManager;
        this.promptManager = promptManager;
        this.ethDataManager = ethDataManager;
        this.bchDataManager = bchDataManager;
        this.currencyState = currencyState;
        this.walletOptionsDataManager = walletOptionsDataManager;
        this.metadataManager = metadataManager;
        this.stringUtils = stringUtils;
        this.shapeShiftDataManager = shapeShiftDataManager;
        this.environmentSettings = environmentSettings;
        this.coinifyDataManager = coinifyDataManager;
        this.exchangeService = exchangeService;
        this.kycStatusHelper = kycStatusHelper;
    }

    private void initPrompts(Context context) {
        settingsDataManager.getSettings()
                .flatMap(settings -> promptManager.getCustomPrompts(context, settings))
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .flatMap(Observable::fromIterable)
                .firstOrError()
                .subscribe(
                        getView()::showCustomPrompt,
                        throwable -> {
                            if (!(throwable instanceof NoSuchElementException)) {
                                Timber.e(throwable);
                            }
                        });
    }

    @Override
    public void onViewReady() {
        if (!accessState.isLoggedIn()) {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            getView().kickToLauncherPage();
        } else {
            logEvents();

            getView().showProgressDialog(R.string.please_wait);

            initMetadataElements();

            doPushNotifications();
        }
    }

    /**
     * Initial setup of push notifications.
     * We don't subscribe to addresses for notifications when creating a new wallet.
     * To accommodate existing wallets we need subscribe to the next available addresses.
     */
    private void doPushNotifications() {
        if (!prefs.has(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED)) {
            prefs.setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        }

        if (prefs.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)) {
            payloadDataManager.syncPayloadAndPublicKeys()
                    .compose(RxUtil.addCompletableToCompositeDisposable(this))
                    .subscribe(() -> {
                        //no-op
                    }, throwable -> Timber.e(throwable));
        }
    }

    void doTestnetCheck() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            currencyState.setCryptoCurrency(CryptoCurrency.BTC);
            getView().showTestnetWarning();
        }
    }

    /*
    // TODO: 24/10/2017  WalletOptions api is also accessed in BuyDataManager - This should be improved soon.
     */
    private void doWalletOptionsChecks() {
        Single.zip(
                walletOptionsDataManager.showShapeshift(
                        payloadDataManager.getWallet().getGuid(),
                        payloadDataManager.getWallet().getSharedKey())
                        .singleOrError(),
                kycStatusHelper.shouldDisplayKyc(),
                (showShapeShift, showKyc) -> showShapeShift || showKyc
        ).observeOn(AndroidSchedulers.mainThread())
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .subscribe(this::setExchangeVisiblity, throwable -> {
                    //Couldn't retrieve wallet options. Not safe to continue
                    Timber.e(throwable);
                    getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                    accessState.logout(applicationContext);
                });
    }

    private void setExchangeVisiblity(boolean showExchange) {
        if (BuildConfig.DEBUG) {
            getView().showHomebrewDebug();
        }

        if (showExchange) {
            getView().showExchange();
        } else {
            getView().hideExchange();
        }
    }

    // Could be used in the future
    @SuppressWarnings("unused")
    private SecurityPromptDialog getWarningPrompt(String message) {
        SecurityPromptDialog prompt = SecurityPromptDialog.newInstance(
                R.string.warning,
                message,
                R.drawable.vector_warning,
                R.string.ok_cap,
                false,
                false);
        prompt.setPositiveButtonListener(view -> prompt.dismiss());
        return prompt;
    }

    void initMetadataElements() {
        metadataManager.attemptMetadataSetup()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .andThen(exchangeRateCompletable())
                .andThen(ethCompletable())
                .andThen(shapeshiftCompletable())
                .andThen(bchCompletable())
                .andThen(feesCompletable())
                .doAfterTerminate(() -> {
                            doWalletOptionsChecks();
                            getView().hideProgressDialog();

                            initPrompts(getView().getActivityContext());

                            if (!prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").isEmpty()) {
                                String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                                prefs.removeValue(PrefsUtil.KEY_SCHEME_URL);
                                getView().onScanInput(strUri);
                            }
                        }
                )
                .subscribe(ignore -> {
                    if (getView().isBuySellPermitted()) {
                        initBuyService();
                    } else {
                        getView().setBuySellEnabled(false, false);
                    }

                    rxBus.emitEvent(MetadataEvent.class, MetadataEvent.SETUP_COMPLETE);
                }, throwable -> {
                    //noinspection StatementWithEmptyBody
                    if (throwable instanceof InvalidCredentialsException || throwable instanceof HDWalletException) {
                        if (payloadDataManager.isDoubleEncrypted()) {
                            // Wallet double encrypted and needs to be decrypted to set up ether wallet, contacts etc
                            getView().showSecondPasswordDialog();
                        } else {
                            logException(throwable);
                        }
                    } else {
                        logException(throwable);
                    }
                });
    }

    private Completable bchCompletable() {
        return bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError(throwable -> {
                    Logging.INSTANCE.logException(throwable);
                    // TODO: 21/02/2018 Reload or disable?
                    Timber.e("Failed to load bch wallet");
                });
    }

    private Completable ethCompletable() {
        return ethDataManager.initEthereumWallet(
                stringUtils.getString(R.string.eth_default_account_label))
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError(throwable -> {
                    Logging.INSTANCE.logException(throwable);
                    // TODO: 21/02/2018 Reload or disable?
                    Timber.e("Failed to load eth wallet");
                });
    }

    private Completable shapeshiftCompletable() {
        return shapeShiftDataManager.initShapeshiftTradeData()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError(throwable -> {
                    Logging.INSTANCE.logException(throwable);
                    // TODO: 21/02/2018 Reload or disable?
                    Timber.e("Failed to load shape shift trades");
                });
    }

    private void logException(Throwable throwable) {
        Logging.INSTANCE.logException(throwable);
        getView().showMetadataNodeFailure();
    }

    private Observable<FeeOptions> feesCompletable() {
        return feeDataManager.getBtcFeeOptions()
                .doOnNext(btcFeeOptions -> dynamicFeeCache.setBtcFeeOptions(btcFeeOptions))
                .flatMap(ignored -> feeDataManager.getEthFeeOptions())
                .doOnNext(ethFeeOptions -> dynamicFeeCache.setEthFeeOptions(ethFeeOptions))
                .flatMap(ignored -> feeDataManager.getBchFeeOptions())
                .doOnNext(bchFeeOptions -> dynamicFeeCache.setBchFeeOptions(bchFeeOptions))
                .compose(RxUtil.applySchedulersToObservable())
                .compose(RxUtil.addObservableToCompositeDisposable(this));
    }

    private Completable exchangeRateCompletable() {
        return exchangeRateFactory.updateTickers()
                .compose(RxUtil.applySchedulersToCompletable())
                .compose(RxUtil.addCompletableToCompositeDisposable(this));
    }

    private void checkForMessages() {
        // TODO: 28/02/2018 There is no point in doing this currently
//        getCompositeDisposable().add(contactsDataManager.fetchContacts()
//                .andThen(contactsDataManager.getContactList())
//                .toList()
//                .flatMapObservable(contacts -> {
//                    if (!contacts.isEmpty()) {
//                        return contactsDataManager.getMessages(true);
//                    } else {
//                        return Observable.just(Collections.emptyList());
//                    }
//                })
//                .subscribe(messages -> {
//                    // No-op
//                }, Timber::e));
    }

    void unPair() {
        getView().clearAllDynamicShortcuts();
        payloadManagerWiper.wipe();
        accessState.logout(applicationContext);
        accessState.unpairWallet();
        appUtil.restartApp(LauncherActivity.class);
        accessState.setPIN(null);
        buyDataManager.wipe();
        ethDataManager.clearEthAccountDetails();
        bchDataManager.clearBchAccountDetails();
        DashboardPresenter.onLogout();
    }

    // Usage commented out for now, until Contacts is back again
    @SuppressWarnings("unused")
    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);
        notificationObservable.compose(RxUtil.addObservableToCompositeDisposable(this))
                .compose(RxUtil.applySchedulersToObservable())
                .subscribe(
                        notificationPayload -> checkForMessages(),
                        Throwable::printStackTrace);
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
        appUtil.deleteQR();
        dismissAnnouncementIfOnboardingCompleted();
    }

    void updateTicker() {
        getCompositeDisposable().add(
                exchangeRateFactory.updateTickers()
                        .subscribe(
                                () -> { /* No-op */ },
                                Throwable::printStackTrace));
    }

    private void logEvents() {
        Logging.INSTANCE.logCustom(new SecondPasswordEvent(payloadDataManager.isDoubleEncrypted()));
    }

    String getCurrentServerUrl() {
        return walletOptionsDataManager.getBuyWebviewWalletLink();
    }

    String getDefaultCurrency() {
        return prefs.getValue(PrefsUtil.DEFAULT_CURRENCY, "USD");
    }

    // Usage commented out for now
    @SuppressWarnings("unused")
    private void initContactsService() {
        String uri = null;
        boolean fromNotification = false;

        if (!prefs.getValue(PrefsUtil.KEY_METADATA_URI, "").isEmpty()) {
            uri = prefs.getValue(PrefsUtil.KEY_METADATA_URI, "");
            prefs.removeValue(PrefsUtil.KEY_METADATA_URI);
        }

        if (prefs.getValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, false)) {
            prefs.removeValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION);
            fromNotification = true;
        }

        final String finalUri = uri;
        if (finalUri != null || fromNotification) {
            getView().showProgressDialog(R.string.please_wait);
        }

        rxBus.emitEvent(ContactsEvent.class, ContactsEvent.INIT);

        if (uri != null) {
            getView().onStartContactsActivity(uri);
        } else if (fromNotification) {
            getView().onStartContactsActivity(null);
        } else {
            checkForMessages();
        }
    }

    private void initBuyService() {
        getCompositeDisposable().add(
                Observable.zip(
                        buyDataManager.getCanBuy(),
                        buyDataManager.isCoinifyAllowed(),
                        Pair::create
                ).subscribe(
                        pair -> {
                            boolean isEnabled = pair.first;
                            boolean isCoinifyAllowed = pair.second;

                            getView().setBuySellEnabled(isEnabled, isCoinifyAllowed);
                            if (isEnabled && !isCoinifyAllowed) {
                                buyDataManager.watchPendingTrades()
                                        .compose(RxUtil.applySchedulersToObservable())
                                        .subscribe(getView()::onTradeCompleted, Throwable::printStackTrace);

                                buyDataManager.getWebViewLoginDetails()
                                        .subscribe(getView()::setWebViewLoginDetails, Throwable::printStackTrace);
                            } else if (isEnabled && isCoinifyAllowed) {
                                notifyCompletedCoinifyTrades();
                            }
                        }, throwable -> {
                            Timber.e(throwable);
                            getView().setBuySellEnabled(false, false);
                        }));
    }

    private void notifyCompletedCoinifyTrades() {
        getCompositeDisposable().add(
                new CoinifyTradeCompleteListener(exchangeService, coinifyDataManager, metadataManager)
                        .getCompletedCoinifyTradesAndUpdateMetaData()
                        .firstElement()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                txHash -> getView().onTradeCompleted(txHash),
                                Timber::e)
        );
    }

    private void dismissAnnouncementIfOnboardingCompleted() {
        if (prefs.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
                && prefs.getValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN, false)) {
            prefs.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true);
        }
    }

    void decryptAndSetupMetadata(String secondPassword) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            getView().showSecondPasswordDialog();
        } else {
            metadataManager.decryptAndSetupMetadata(environmentSettings.getBitcoinNetworkParameters(), secondPassword)
                    .compose(RxUtil.addCompletableToCompositeDisposable(this))
                    .subscribe(() -> appUtil.restartApp(LauncherActivity.class), Throwable::printStackTrace);
        }
    }

    void setCryptoCurrency(CryptoCurrency cryptoCurrency) {
        currencyState.setCryptoCurrency(cryptoCurrency);
    }

    void routeToBuySell() {
        buyDataManager.isCoinifyAllowed()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(coinifyAllowed -> {

                    if (coinifyAllowed) {
                        getView().onStartBuySell();
                    } else {
                        getView().onStartLegacyBuySell();
                    }
                }, Throwable::printStackTrace);
    }
}
